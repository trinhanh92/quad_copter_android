package trieudo.android.drone;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Do Trieu on 5/18/2016.
 */
public class HttpPostTask extends AsyncTask<String, String, String>
{
    public enum TaskStatus
    {
        Pending,
        Running,
        Successful,
        UnableToConnect,
        IncorrectInfo,
        NoSupported
    }

    public interface TaskCompletionListener
    {
        void Completed(TaskStatus status);
    }

    private Context context;
    private String url;
    private int numOfParams;
    private TaskCompletionListener taskCompletionListener;
    private TaskStatus status;

    public JSONObject Response;

    public HttpPostTask(Context context, String url, int numOfParams)
    {
        this.context = context;
        this.url = url;
        this.numOfParams = numOfParams;
        this.Response = null;
        this.status = TaskStatus.Pending;
    }

    public void setTaskCompletionListener(TaskCompletionListener listener)
    {
        this.taskCompletionListener = listener;
    }

    @Override
    protected String doInBackground(String... params)
    {
        // Create a new HttpClient and Post Header
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
		HttpConnectionParams.setSoTimeout(httpParams, 1000);
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpPost httppost = new HttpPost(this.url);
        HttpResponse httpResponse;
        HttpEntity httpEntity;
        JSONObject dataJSON;
        this.status = TaskStatus.Running;
        try
        {
            // add data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            String paramsBeforeHash = "";
            for (int i = 0; i < this.numOfParams; i++)
            {
                nameValuePairs.add(new BasicNameValuePair(params[i * 2], params[i * 2 + 1]));
                paramsBeforeHash += params[i * 2 + 1];
            }
            nameValuePairs.add(new BasicNameValuePair("sig", this.buildSignature(this.context, paramsBeforeHash)));

            // set entity
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // execute http post
            httpResponse = httpclient.execute(httppost);
            httpEntity = httpResponse.getEntity();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            dataJSON = new JSONObject(/*EntityUtils.toString(httpEntity)*/json);
            if (dataJSON.getInt("error") == 0)
            {
                String str = dataJSON.getString("data");
                if(!TextUtils.isEmpty(str) && !str.equalsIgnoreCase("null"))
                    this.Response = dataJSON.getJSONObject("data");
                this.status = TaskStatus.Successful;
            }
            else
                this.status = TaskStatus.NoSupported;

        } catch (UnsupportedEncodingException e) {
            // never occurs
        } catch (JSONException e) {
            this.status = TaskStatus.IncorrectInfo;
        } catch (ClientProtocolException e) {
            // never occurs
        } catch (IOException e) {
            this.status = TaskStatus.UnableToConnect;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result)
    {
        if (this.taskCompletionListener != null)
            this.taskCompletionListener.Completed(this.status);
    }

    private String buildSignature(Context context, String params)
    {
        // add secret key to this params
        String signatureBeforeHash = params + context.getResources().getString(R.string.SecretKey);
        // get MD5 hash from this params
        try
        {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(signatureBeforeHash.getBytes());
            byte messageDigest[] = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest)
            {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e)
        {
            return null;
        }
    }
}