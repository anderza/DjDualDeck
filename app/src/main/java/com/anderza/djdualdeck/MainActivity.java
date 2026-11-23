package com.anderza.djdualdeck;
import android.app.Activity;
import android.provider.DocumentsContract;
import android.database.Cursor;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.view.WindowInsets;
import android.graphics.Insets;
import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> pending;
 private static final int CHOOSE=900, FOLDER=901;
 private String targetDeck="A";
 private JSONArray folders(){try{return new JSONArray(getPreferences(0).getString("saved_folders","[]"));}catch(Exception e){return new JSONArray();}}
 private void save(JSONArray a){getPreferences(0).edit().putString("saved_folders",a.toString()).apply();}
 private void sendFolders(){String json=folders().toString();web.post(()->web.evaluateJavascript("window.djFoldersUpdated("+JSONObject.quote(json)+")",null));}
 private void sendTracks(String deck,String uri){try{Uri tree=Uri.parse(uri);Uri dir=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));JSONArray tracks=new JSONArray();scan(dir,tree,tracks,0);String payload=tracks.toString();web.post(()->web.evaluateJavascript("window.djFolderLoaded("+JSONObject.quote(deck)+","+JSONObject.quote(payload)+")",null));}catch(Exception e){String msg=e.getMessage()==null?"Ошибка доступа к папке":e.getMessage();web.post(()->web.evaluateJavascript("window.djFolderError("+JSONObject.quote(msg)+")",null));}}
 private void scan(Uri dir,Uri tree,JSONArray tracks,int depth){if(depth>5||tracks.length()>2000)return;Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(dir));try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(c==null)return;while(c.moveToNext()){String id=c.getString(0),name=c.getString(1),mime=c.getString(2);Uri child=DocumentsContract.buildDocumentUriUsingTree(tree,id);if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){scan(child,tree,tracks,depth+1);}else if(name!=null&&name.matches("(?i).*\\.(mp3|wav|ogg|flac|m4a|aac|opus|webm)$")){JSONObject o=new JSONObject();o.put("name",name);o.put("url",child.toString());o.put("path",name);tracks.put(o);}}}catch(Exception ignored){}}
 public class FolderBridge {
  @JavascriptInterface public void choose(String deck){runOnUiThread(()->{targetDeck="B".equals(deck)?"B":"A";Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,FOLDER);});}
  @JavascriptInterface public void list(){runOnUiThread(()->sendFolders());}
  @JavascriptInterface public void open(String deck,String uri){new Thread(()->sendTracks("B".equals(deck)?"B":"A",uri)).start();}
  @JavascriptInterface public void remove(String uri){runOnUiThread(()->{JSONArray old=folders(),a=new JSONArray();for(int i=0;i<old.length();i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!uri.equals(x.optString("uri")))a.put(x);}save(a);sendFolders();});}
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);
  web=new WebView(this);web.setBackgroundColor(0xff080d13);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setMediaPlaybackRequiresUserGesture(false);
  web.setWebViewClient(new WebViewClient());
  web.addJavascriptInterface(new FolderBridge(),"AndroidFolder");
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams p){
    if(pending!=null)pending.onReceiveValue(null);pending=callback;
    try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
     i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
     i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
     startActivityForResult(i,CHOOSE);return true;
    }catch(Exception ex){pending=null;callback.onReceiveValue(null);return false;}
   }
  });
  FrameLayout root=new FrameLayout(this);
  root.addView(web,new FrameLayout.LayoutParams(-1,-1));
  root.setOnApplyWindowInsetsListener((view,windowInsets)->{
   if(Build.VERSION.SDK_INT>=30){
    Insets in=windowInsets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    view.setPadding(in.left,in.top,in.right,in.bottom);
   }else{
    view.setPadding(windowInsets.getSystemWindowInsetLeft(),windowInsets.getSystemWindowInsetTop(),windowInsets.getSystemWindowInsetRight(),windowInsets.getSystemWindowInsetBottom());
   }
   return windowInsets;
  });
  if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  getWindow().setStatusBarColor(0xff090d13);
  getWindow().setNavigationBarColor(0xff090d13);
  setContentView(root);
  root.requestApplyInsets();
  web.loadUrl("file:///android_asset/index.html");
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
  if(req==FOLDER){if(result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}JSONArray old=folders(),a=new JSONArray();String key=uri.toString();String name=Uri.decode(uri.getLastPathSegment());try{JSONObject entry=new JSONObject();entry.put("uri",key);entry.put("name",name);a.put(entry);for(int i=0;i<old.length();i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!key.equals(x.optString("uri")))a.put(x);}save(a);}catch(Exception ignored){}sendFolders();new Thread(()->sendTracks(targetDeck,key)).start();}return;}
  if(req!=CHOOSE || pending==null)return;
  Uri[] uris=null;
  if(result==RESULT_OK && data!=null){
   if(data.getClipData()!=null){int n=data.getClipData().getItemCount();uris=new Uri[n];for(int i=0;i<n;i++)uris[i]=data.getClipData().getItemAt(i).getUri();}
   else if(data.getData()!=null)uris=new Uri[]{data.getData()};
  }
  pending.onReceiveValue(uris);pending=null;
 }
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);}
 @Override protected void onDestroy(){if(pending!=null)pending.onReceiveValue(null);if(web!=null)web.destroy();super.onDestroy();}
}