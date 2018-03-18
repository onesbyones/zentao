package com.zentao;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class ZenTaoUtil {

	private String username;
	private String zentaosid;

	// 禅道用户名、密码登录
	public ZenTaoUtil(String username, String password) throws Exception {
		this.username = username;

		ZenTaoLogin zt = new ZenTaoLogin();
		this.zentaosid = zt.getZentaoID();

		if (this.zentaosid.isEmpty()) {
			throw new Exception("**************zentaosid not found **************\n");
		}

		zt.zentaoLogin(username, password, this.zentaosid);
	}

	/**
	 * 标记禅道结果：忽略（n/a）、通过（pass）、失败（fail）
	 * 
	 * @param moduleID
	 *            指定模块的ID
	 * @param caseID
	 *            禅道中用例编号ID
	 * @param resultList
	 *            结果：pass、fail、n/a，用例中对应的每一步结果必须填入，必须和步骤数一致
	 * @param reasonList
	 *            实际情况：用例中的每一步情况都必须填写，必须和步骤数一致
	 */
	public boolean caseTagByHttp(String moduleID, String caseID, String[] resultList, String[] reasonList)
			throws Exception {

		if (moduleID.isEmpty() || caseID.isEmpty()) {
			throw new Exception("**************moduleID 或者caseID是空的， 请检查用例标记参数是否正确**************");
		}

		// 分隔符
		String tempBoundary = "----WebKitFormBoundaryXPZRoJ4Bj9KNIVgc";

		// 根据moduleID、caseID查询出caseUrl
		HtmlUtil hu = new HtmlUtil();
		String tempCaseUrl = hu.getCaseUrlByModuleID(this.zentaosid, moduleID, caseID);
		System.out.println("**************tempCaseUrl: \n" + tempCaseUrl + "\n**************");
		String caseUrl = tempCaseUrl.split("\\;")[1];

		if (caseUrl.isEmpty()) {
			throw new Exception("caseUrl获取失败");
		}

		String versionID = tempCaseUrl.split("\\;")[2];
		// System.out.println("**************caseUrl: " + caseUrl +
		// "\n**************");
		// System.out.println("**************versionID: " + versionID +
		// "\n**************");

		// 根据caseUrl查询stepID、realsID
		HashMap<String, String> paramsMap = hu.getCaseParamByUrl(caseID, caseUrl, moduleID, this.zentaosid);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(Param.HOST + caseUrl);

			httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			httpPost.setHeader("Accept-Encoding", "gzip, deflate");
			httpPost.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
			httpPost.setHeader("Content-Type", "multipart/form-data; boundary=" + tempBoundary);// 还需要加上一个键值对：boundary=--------------------------161370145786553934711274
			httpPost.setHeader("Cookie", "qaBugOrder=id_desc; keepLogin=on; za=" + this.username
					+ "; preBranch=0; lang=zh-cn; theme=default; bugModule=0; caseSide=show; zp=f0947ae6bf9cad7c1f20b26ab7359d70f15014e2; checkedItem=; lastProject=7; preProductID=2; lastProduct=2; caseModule="
					+ moduleID + "; selfClose=0; windowWidth=1792; windowHeight=699; zentaosid=" + this.zentaosid);

			StringBuffer sb = new StringBuffer();
			for (Map.Entry<String, String> temp : paramsMap.entrySet()) {
				// System.out.println(temp.getKey() + "-->" + temp.getValue());

				if (temp.getKey().equalsIgnoreCase(caseID)) {

					String[] tempArray = temp.getValue().split("\\;");

					// 判断长度
					if (tempArray.length != resultList.length || tempArray.length != reasonList.length) {
						throw new Exception(
								"**************resultList[] 或者 reasonList[] 长度 与 用例执行步骤数不一致，请检查配置！！**************");
					}

					for (int i = 0; i < tempArray.length; i++) {

						String tempStepID = tempArray[i].split(",")[0];
						String tempRealsID = tempArray[i].split(",")[1];

						if (!Arrays.asList(Param.RESULT_LIST).contains(resultList[i])) {
							throw new Exception("**************resultList[] 中包含非法的字段，请检查配置！！**************");
						}

						sb.append("--" + tempBoundary + "\n" + "Content-Disposition: form-data; name=\"" + tempStepID
								+ "\"" + "\n\n" + resultList[i] + "\n");
						sb.append("--" + tempBoundary + "\n" + "Content-Disposition: form-data; name=\"" + tempRealsID
								+ "\"" + "\n\n" + reasonList[i] + "\n");
					}
				}

			}

			sb.append("--" + tempBoundary + "\n" + "Content-Disposition: form-data; name=\"case\"" + "\n\n" + caseID
					+ "\n");
			sb.append("--" + tempBoundary + "\n" + "Content-Disposition: form-data; name=\"version\"" + "\n\n"
					+ versionID + "\n");
			sb.append("--" + tempBoundary + "--\n");

			// System.out.println("**************post body: \n" + sb.toString()
			// + "\n**************");

			StringEntity postEntity = new StringEntity(sb.toString(), "UTF-8");
			httpPost.setEntity(postEntity);

			CloseableHttpResponse response = httpClient.execute(httpPost);
			HttpEntity responseEntity = response.getEntity();
			System.out
					.println("**************case flag: \n" + EntityUtils.toString(responseEntity) + "\n**************");

			if (response.getStatusLine().getStatusCode() == 200) {
				return true;
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

		return false;

	}

}
