import java.util.HashSet;
import java.util.Set;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

public class html {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "http://sports.baidu.com/";
		String url2 = "http://guonei.news.baidu.com/";
		String url3 = "http://lady.baidu.com/";
		String url4 = "http://news.baidu.com/";
		//extractKeyWordText(url,keyword);
		extracLinks(url, 5);
		extracLinks(url2, 2);
		extracLinks(url4, 1);
		extracLinks(url3, 10);
	}
	
		// 获取一个网页上所有的链接和图片链接
		public static void extracLinks(String url, int id) {
			Set<String> links = new HashSet<String>();
			try {
				Parser parser = new Parser(url);
				// utf-8 gbk gb2312
				parser.setEncoding("gb2312");
				//得到所有经过过滤的标签
				NodeList list = parser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
				for (int i = 0; i < list.size(); i++) {
					Node tag = list.elementAt(i);
					if (tag instanceof LinkTag)//<a> 标签 
					{
						LinkTag link = (LinkTag) tag;
						String linkUrl = link.getLink();//url
						String text = link.getLinkText();//链接文字
						if(accept2(linkUrl) || accept3(linkUrl))
						{
							//System.out.println(linkUrl + "**********" + text);
							links.add(linkUrl);
						}
					}
				}
			} catch (ParserException e) {
						e.printStackTrace();
			}
			int i=0;
			newsparser news = new newsparser();
			for (String link : links) {
	            i++;
	            if(i<7)
	            {
	            	System.out.println(link);
	            	news.parser(link, id); //解析连接
	            }
	        }
			}
	//"http://sports.(sohu|163|sina|qq).com(.cn|/13|/a)?/[\\w]+/(n\\w|\\w|\\W)+(.shtml|.htm|.html)"	
	/*public static boolean accept(String url) {
        /*if (url.matches("http://sports.163.com/13/[\\d]{4}/[\\d]{2}/[\\w]+.html") || 
        		url.matches("http://sports.sina.com.cn/[\\w]+/(\\W|\\w)+/[\\d]+.shtml") || 
        		url.matches("http://sports.qq.com/a/[\\d]{8}/[\\d]{6}.htm") ||
        		url.matches("http://sports.sohu.com/[\\d]+/n[\\d]+.shtml"))  {*/
		/*if(url.matches("http://sports.(sohu|163|sina).com(.cn|/13|/a)?/[\\w]+/(n\\w|\\w|\\W)+(.shtml|.htm|.html)")){
              return true;
        } else {
              return false;
        }
    }*/
	public static boolean accept2(String url) {	
		if(url.matches("http://news.(sohu|163|sina).com(.cn|/13|/a)?/[\\w]+/(n\\w|\\w|\\W)+(.shtml|.htm|.html)") || 
				url.matches("http://www.chinanews.com/[\\w]+/[\\w|\\W]+/[\\d]+.shtml")){
              return true;
        } else {
              return false;
        }
    }
	public static boolean accept3(String url) {	
		if(url.matches("http://[^(sz)]+.(sohu|163|sina).com(.cn|/13|/a)?/[\\w]+/(n\\w|\\w|\\W)+(.shtml|.htm|.html)")){ 
				//url.matches("http://www.yangtse.com/system/(\\w|\\W)+/[\\d]+.shtml")
              return true;
        } else {
              return false;
        }
    }
}
