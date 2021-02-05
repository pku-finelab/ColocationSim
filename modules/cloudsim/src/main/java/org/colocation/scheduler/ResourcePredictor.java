package org.colocation.scheduler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by wkj on 2019/6/5.
 */
public class ResourcePredictor {
    private String url;
    public ResourcePredictor(String url) {
        this.url = url;
    }
    public ArrayList<Double> predict(ArrayList<Double> data, int preLen) {
        JSONObject jsonObject = new JSONObject();
        StringBuffer sb=new StringBuffer();
        ArrayList<Double> res = new ArrayList<>();
        try {
            // 创建url资源
            URL url = new URL(this.url);
            // 建立http连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置允许输出
            conn.setDoOutput(true);
            // 设置允许输入
            conn.setDoInput(true);
            // 设置不用缓存
            conn.setUseCaches(false);
            // 设置传递方式
            conn.setRequestMethod("POST");
            // 设置维持长连接
            conn.setRequestProperty("Connection", "Keep-Alive");
            // 设置文件字符集:
            conn.setRequestProperty("Charset", "UTF-8");
            // 转换为字节数组
            jsonObject.put("history", data);
            jsonObject.put("preLen", preLen);
            String jsonStr = jsonObject.toJSONString();
            byte[] postData = jsonStr.getBytes();
            // 设置文件长度
            conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
            // 设置文件类型:
            conn.setRequestProperty("contentType", "application/json");
            // 开始连接请求
            conn.connect();
            OutputStream out = new DataOutputStream(conn.getOutputStream()) ;
            // 写入请求的字符串
            out.write(postData);
            out.flush();
            out.close();

            System.out.println(conn.getResponseCode());

            // 请求返回的状态
            if (HttpURLConnection.HTTP_OK == conn.getResponseCode() ) {
                System.out.println("remote predictor service Success");
                // 请求返回的数据
                InputStream in1 = conn.getInputStream();
                try {
                    String readLine=new String();
                    BufferedReader responseReader=new BufferedReader(new InputStreamReader(in1,"UTF-8"));
                    while((readLine=responseReader.readLine())!=null){
                        sb.append(readLine).append("\n");
                    }
                    responseReader.close();
                    JSONObject resJson = JSONObject.parseObject(sb.toString());
                    JSONArray jsonArray = resJson.getJSONArray("res");
                    for (int i = 0; i <jsonArray.size(); i ++) {
                        double preData = jsonArray.getDouble(i);
                        res.add(preData);
                    }
                    return  res;

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                System.out.println("error++");

            }

        } catch (Exception e) {
        }
        return  null;

    }
    public ArrayList<Double> predict2(ArrayList<Double> data, int preLen) {
        try {
            JSONObject postBody = new JSONObject();
            postBody.put("history", data);
            postBody.put("preLen", preLen);
            HttpResponse<String> response = Unirest.post("http://192.168.235.136:2019/predict")
                    .header("Content-Type", "application/json")
                    .header("cache-control", "no-cache")
                    .header("Postman-Token", "0e0936cf-4f95-4944-b53d-4a6ea11e15b1")
                    .body(postBody.toJSONString())
                    .asString();
            JSONObject resJson = JSONObject.parseObject(response.getBody());
            JSONArray jsonArray = resJson.getJSONArray("res");

            ArrayList<Double> res = new ArrayList<>();
            for (int i = 0; i <jsonArray.size(); i ++) {
                double preData = jsonArray.getDouble(i);
                res.add(preData);
            }
            return  res;
        } catch (UnirestException e ) {
            System.out.println("error++");
            return  null;
        }
    }
}
