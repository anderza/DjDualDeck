package com.anderza.djdualdeck;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> pending;
 private static final int CHOOSE=900;
 @Override public void onCreate(Bundle b){super.onCreate(b);
  web=new WebView(this);web.setBackgroundColor(0xff080d13);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setMediaPlaybackRequiresUserGesture(false);
  web.setWebViewClient(new WebViewClient());
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
  setContentView(web);web.loadUrl("file:///android_asset/index.html");
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
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