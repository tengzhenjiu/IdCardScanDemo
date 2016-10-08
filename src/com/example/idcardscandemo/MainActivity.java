package com.example.idcardscandemo;

import java.io.FileNotFoundException;
import java.io.InputStream;

import com.example.idcardscandemo.utils.HttpUtil;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String tag = "MainActivity";
	private int REQUEST_CODE = 1;
	private TextView tvResult;
	private static byte[] bytes;
	private static String extension;
	private final int IMPORT_CODE = 1;
	private final int TAKEPHOTO_CODE = 2;
	private LinearLayout ll_progress;
	public static final String action = "idcard.scan";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		initView();
	}

	private void initView() {
		tvResult = (TextView) findViewById(R.id.tv_result);
		ll_progress = (LinearLayout) findViewById(R.id.ll_progress);
	}

	/**
	 * 导入图片
	 * 
	 * @param view
	 */
	public void choiseImg(View view) {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQUEST_CODE);
	}

	/**
	 * 拍照
	 */
	public void takePhoto(View view) {
		Intent intent = new Intent(MainActivity.this, ACameraActivity.class);
		startActivityForResult(intent, TAKEPHOTO_CODE);
	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent data) {
		super.onActivityResult(arg0, arg1, data);
		if (data == null) {
			return;
		}
		Uri uri = data.getData();
		if (arg1 == Activity.RESULT_OK) {
			switch (arg0) {
			case IMPORT_CODE:
				if (uri == null) {
					return;
				}
				try {
					String uriPath = getUriAbstractPath(uri);
					extension = getExtensionByPath(uriPath);
					InputStream is = getContentResolver().openInputStream(uri);
					bytes = HttpUtil.Inputstream2byte(is);
					Log.d("bytes:  ", bytes.length + "");
					if (!(bytes.length > (1000 * 1024 * 5))) {
						new MyAsynTask().execute();
					} else {
						Toast.makeText(MainActivity.this, "图片太大！！！", 1).show();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
			case TAKEPHOTO_CODE:
				if (tvResult.getVisibility() == View.GONE) {
					tvResult.setVisibility(View.VISIBLE);
				}
				tvResult.setText("");
				String result = data.getStringExtra("result");
				Log.d(tag, "result:  " + result);
				tvResult.setText(result);
				break;
			}
		}
	}

	/**
	 * 根据路径获取文件扩展名
	 * 
	 * @param path
	 */
	private String getExtensionByPath(String path) {
		if (path != null) {
			return path.substring(path.lastIndexOf(".") + 1);
		}
		return null;
	}

	/**
	 * 根据uri获取绝对路径
	 * 
	 * @param uri
	 */
	private String getUriAbstractPath(Uri uri) {
		{
			// can post image
			String[] proj = { MediaStore.Images.Media.DATA };
			Cursor cursor = managedQuery(uri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();

			return cursor.getString(column_index);
		}
	}

	class MyAsynTask extends AsyncTask<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			ll_progress.setVisibility(View.VISIBLE);
		}

		@Override
		protected String doInBackground(Void... params) {
			return startScan();
		}

		@Override
		protected void onPostExecute(String result) {
			System.out.println("result:   " + result);
			if (result != null) {
				ll_progress.setVisibility(View.GONE);
				handleResult(result);
			}
		}

	}

	/**
	 * 处理服务器返回的结果
	 * 
	 * @param result
	 */
	private void handleResult(String result) {
		tvResult.setVisibility(View.VISIBLE);
		tvResult.setText(result);
	}

	public static String startScan() {
		String xml = HttpUtil.getSendXML(action, extension);
		return HttpUtil.send(xml, bytes);
	}
}
