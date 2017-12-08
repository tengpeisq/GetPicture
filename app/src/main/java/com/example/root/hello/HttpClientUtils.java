package com.example.root.hello;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClientUtils {

    public static void test(File file) {

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(3000l, TimeUnit.MILLISECONDS)
                .readTimeout(3000l, TimeUnit.MILLISECONDS)
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("File", file.getName(), RequestBody.create(MediaType.parse("text/plain"), file))
                .addFormDataPart("Text", "text")
                .build();

        Request request = new Request.Builder()
                .url("http://www.hzynyp.com/QS/testHandler.html")
                .post(requestBody)
                .build();


        Call call = okHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("我是异步线程,线程Id为:" + Thread.currentThread().getId());
            }
        });

    }



/*
    public final static String Method_POST = "POST";

    public final static String Method_GET = "GET";


    public static String submitForm(MultipartForm form)
    {

        // 返回字符串
        String responseStr = "";

        // 创建HttpClient实例
        HttpClient httpClient = new DefaultHttpClient();

        try
        {
            // 实例化提交请求
            HttpPost httpPost = new HttpPost(form.getAction());

            // 创建MultipartEntityBuilder
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

            // 追加普通表单字段
            Map<String, String> normalFieldMap = form.getNormalField();
            for (Iterator<Entry<String, String>> iterator = normalFieldMap.entrySet().iterator(); iterator.hasNext();)
            {
                Entry<String, String> entity = iterator.next();
                entityBuilder.addPart(entity.getKey(), new StringBody(entity.getValue(), ContentType.create("text/plain", Consts.UTF_8)));
            }

            // 追加文件字段
            Map<String, File> fileFieldMap = form.getFileField();
            for (Iterator<Entry<String, File>> iterator = fileFieldMap.entrySet().iterator(); iterator.hasNext();)
            {
                Entry<String, File> entity = iterator.next();
                entityBuilder.addPart(entity.getKey(), new FileBody(entity.getValue()));
            }

            // 设置请求实体
            httpPost.setEntity(entityBuilder.build());

            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            // 取得响应数据
            HttpEntity resEntity = response.getEntity();
            if (200 == statusCode)
            {
                if (resEntity != null)
                {
                    responseStr = EntityUtils.toString(resEntity);
                }
            }
        } catch (Exception e)
        {
            System.out.println("提交表单失败，原因：" + e.getMessage());
        } finally
        {

            httpClient.getConnectionManager().shutdown();
        }

        return responseStr;
    }


    public class MultipartForm implements Serializable
    {

        private static final long serialVersionUID = -2138044819190537198L;


        private String action = "";


        private String method = "POST";


        private Map<String, String> normalField = new LinkedHashMap<String, String>();


        private Map<String, File> fileField = new LinkedHashMap<String, File>();

        public String getAction()
        {
            return action;
        }

        public void setAction(String action)
        {
            this.action = action;
        }

        public String getMethod()
        {
            return method;
        }

        public void setMethod(String method)
        {
            this.method = method;
        }

        public Map<String, String> getNormalField()
        {
            return normalField;
        }

        public void setNormalField(Map<String, String> normalField)
        {
            this.normalField = normalField;
        }

        public Map<String, File> getFileField()
        {
            return fileField;
        }

        public void setFileField(Map<String, File> fileField)
        {
            this.fileField = fileField;
        }

        public void addFileField(String key, File value)
        {
            fileField.put(key, value);
        }

        public void addNormalField(String key, String value)
        {
            normalField.put(key, value);
        }
    }

*/
}
