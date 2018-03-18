package com.zentao;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class ZenTaoLogin {

	/**
	 * cookie中添加zentaosid，根据zentaosid登录，否则无法登录查询
	 * 
	 * @param username
	 *            禅道登录用户名
	 * @param password
	 *            禅道登录密码
	 * @param zentaosid
	 *            服务器校验是否过期，过期无法登录
	 * @return void
	 * @throws Exception
	 */

	public void zentaoLogin(String username, String password, String zentaosid) throws Exception {

		if (username.isEmpty() || password.isEmpty() || zentaosid.isEmpty()) {
			throw new Exception("请检查禅道登录参数");
		}

		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(Param.LOGIN_URL);

			// 设置请求头 -->后续估计需要采用登录接口配置
			httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			httpPost.setHeader("Accept-Encoding", "gzip, deflate");
			httpPost.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
			httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");// 还需要加上一个键值对：boundary=--------------------------161370145786553934711274
			httpPost.setHeader("Cookie",
					"ang=zh-cn; theme=default; windowWidth=1920; windowHeight=974; zentaosid=" + zentaosid);

			// 配置 application/x-www-form-urlencoded 类型请求体 -->调试时先写死
			String postBody = "account=" + username + "&password=" + password + "&referer=" + Param.REFER_URL;
			StringEntity postEntity = new StringEntity(postBody, "UTF-8");
			httpPost.setEntity(postEntity);

			// 发起请求
			CloseableHttpResponse response = httpClient.execute(httpPost);
			// 获取响应体
			HttpEntity responseEntity = response.getEntity();
			// 解析响应体到字符串
			System.out.println(
					"**************login response: \n" + EntityUtils.toString(responseEntity) + "\n**************");

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭连接,释放资源
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 从禅道首页获取zentaosid，用于登录验证
	 * 
	 * @return String
	 */

	public String getZentaoID() {
		CloseableHttpClient httpClient = HttpClients.createDefault();

		try {
			HttpGet httpGet = new HttpGet(Param.HOST + Param.REFER_URL);
			httpGet.setHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			httpGet.setHeader("Accept-Encoding", "gzip, deflate");
			httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
			CloseableHttpResponse response = httpClient.execute(httpGet);

			// 遍历响应头部
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				// System.out.println(header.getName() + "--->" +
				// header.getValue());
				String tempStr = header.getValue();
				if (tempStr.contains("zentaosid")) {
					return tempStr.split(";")[0].split("=")[1];
				}
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭连接,释放资源
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return "";
	}

}
