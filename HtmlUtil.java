package com.zentao;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlUtil {

	/**
	 * 根据传入的caseID，匹配出case对应的URL
	 * 
	 * @param caseID
	 *            禅道中对应的用例编号
	 * @param moduleID
	 *            指定用例模块的ID，需要先定义好
	 * @param zentaosid
	 *            禅道登录ID
	 * @return String: caseID+caseURL+versionID
	 * 
	 */

	public String getCaseUrlByModuleID(String zentaosid, String moduleID, String caseID) {

		String moduleUrl = "";
		switch (moduleID) {
		case "801":
			moduleUrl = Param.MODULE_URL_801;
			break;
		default:
			break;
		}

		StringBuffer sb = new StringBuffer();

		CloseableHttpClient httpClient = HttpClients.createDefault();

		try {
			// System.out.println("**************moduleUrl: \n" + moduleUrl +
			// "\n**************");

			HttpGet httpGet = new HttpGet(moduleUrl);
			httpGet.setHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			httpGet.setHeader("Accept-Encoding", "gzip, deflate");
			httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
			httpGet.setHeader("Cookie",
					"lang=zh-cn; theme=default; preBranch=0; preProductID=2; bugModule=0; lastProduct=2; qaBugOrder=id_desc; taskCaseModule=538; caseModule="
							+ moduleID + "; selfClose=0; windowWidth=1903; windowHeight=974; zentaosid=" + zentaosid);

			CloseableHttpResponse response = httpClient.execute(httpGet);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String htmlResponseStr = EntityUtils.toString(entity, "UTF-8");

					Document docm = Jsoup.parse(htmlResponseStr);
					Elements tbodyEles = docm.getElementsByTag("tbody");
					for (Element element : tbodyEles) {
						Elements trElements = element.getElementsByTag("td");

						for (Element tdElement : trElements) {
							if (!tdElement.getElementsByClass("text-right").isEmpty()) {
								Elements aElements = tdElement.getElementsByTag("a");
								for (Element aElement : aElements) {
									if (aElement.attr("title").equalsIgnoreCase("执行")) {
										String tempUrl = aElement.attr("href");
										// System.out.println("URL为：" +
										// tempUrl);
										String[] temps = tempUrl.split("-");
										String tempCaseID = temps[3];// caseID
										String tempCaseVersion = temps[temps.length - 1].split("\\.")[0];// 标记接口中的cookies对应的version值
										if (caseID.equalsIgnoreCase(temps[3])) {
											return sb.append(tempCaseID).append(";" + tempUrl)
													.append(";" + tempCaseVersion).toString();
										}
									}
								}
							}
						}
					}
				}

			} finally {
				response.close();
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

	/**
	 * 通过caseID、caseURL，获取case需要标记的依赖参数。
	 * 
	 * @param url:
	 *            根据caseID查询出来的caseUrl参数
	 * 
	 * @return Map: caseID ->stepID,realsID;stepID,realsID;
	 */
	public HashMap<String, String> getCaseParamByUrl(String caseID, String caseUrl, String moduleID, String zentaosid) {

		HashMap<String, String> caseParamMap = new HashMap<>();

		CloseableHttpClient httpClient = HttpClients.createDefault();

		try {
			HttpGet httpGet = new HttpGet(Param.HOST + caseUrl);

			httpGet.setHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			httpGet.setHeader("Accept-Encoding", "gzip, deflate");
			httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
			httpGet.setHeader("Cookie",
					"lang=zh-cn; theme=default; preBranch=0; preProductID=2; bugModule=0; lastProduct=2; qaBugOrder=id_desc; taskCaseModule=538; caseModule="
							+ moduleID + "; selfClose=0; windowHeight=974; windowWidth=1903; zentaosid=" + zentaosid);

			CloseableHttpResponse response = httpClient.execute(httpGet);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String htmlResponseStr = EntityUtils.toString(entity, "UTF-8");
					Document docm = Jsoup.parse(htmlResponseStr);
					Elements trEles = docm.getElementsByClass("step step-step");
					StringBuffer sb = new StringBuffer();
					for (Element trElement : trEles) {
						String tempStepID = null;
						String tempRealsID = null;
						Elements selectEles = trElement.getElementsByClass("form-control");
						for (Element selectElement : selectEles) {
							if (selectElement.attr("class").equalsIgnoreCase("form-control")) {
								tempStepID = selectElement.attr("name");
								// System.out.println("caseID -> tempStepID:" +
								// tempStepID);
							}
							if (selectElement.attr("class").equalsIgnoreCase("form-control autosize")) {
								tempRealsID = selectElement.attr("name");
								// System.out.println("caseID -> tempRealsID:" +
								// tempRealsID);
							}
						}

						sb.append(tempStepID + "," + tempRealsID + ";");

					}

					caseParamMap.put(caseID, sb.toString());

				}

			} finally {
				response.close();
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

		return caseParamMap;
	}

}
