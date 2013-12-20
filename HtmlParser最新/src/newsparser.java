import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;

import org.htmlparser.tags.Div;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.tags.Span;


public class newsparser {
	private Parser parser = null;   //用于分析网页的分析器。
    //private List newsList = new ArrayList();    //暂存新闻的List；
    private NewsBean bean = new NewsBean();
    private ConnectionManager manager = null;    //数据库连接管理器。
    private PreparedStatement pstmt = null;
    public newsparser() {
    }
    
    /*public List getNewsList(final NewsBean newsBean) {
        List list = new ArrayList();
        String newstitle = newsBean.getNewsTitle();
        String newsauthor = newsBean.getnewszy();
        String newscontent = newsBean.getNewsContent();
        String newsdate = newsBean.getNewsDate();
        list.add(newstitle);
        list.add(newsauthor);
        list.add(newscontent);
        list.add(newsdate);
        return list;
    }*/
    
    public void setNews(String newsTitle, String newszy, String newsContent, String newsDate, String url) {
        bean.setNewsTitle(newsTitle);
        bean.setnewszy(newszy);
        bean.setNewsContent(newsContent);
        bean.setNewsDate(newsDate);
        bean.setNewsURL(url);
    }
    
    protected void newsToDataBase(final int id) {

        //建立一个线程用来执行将新闻插入到数据库中。
        Thread thread = new Thread(new Runnable() {

            public void run() {
                boolean sucess = saveToDB(bean,id);
                if (sucess != false) {
                    System.out.println("插入数据失败");
                }
            }
        });
        thread.start();
    }
    
    public boolean saveToDB(NewsBean bean,int id) {
        boolean flag = true;
        String sql = "insert into t_news(nid,cid,title,digest,body,source,ptime,imgsrc,deleted) values(?,?,?,?,?,?,?,?,?)";
        manager = new ConnectionManager();
        String titleLength = bean.getNewsTitle();
        if (titleLength.length() > 50) {  //标题太长的新闻不要。
            return flag;
        }
        try {
            pstmt = manager.getConnection().prepareStatement(sql);
            
            //pstmt.setString(1, "12");
            pstmt.setString(1, null);
            pstmt.setLong(2, id);
            pstmt.setString(3, bean.getNewsTitle());
            pstmt.setString(4, bean.getnewszy());
            pstmt.setString(5, bean.getNewsContent());
            //pstmt.setString(6, bean.getNewsURL());
            pstmt.setString(6, "互联网");
            pstmt.setString(7, bean.getNewsDate());
            pstmt.setString(8, null);
            pstmt.setString(9, "0");
            flag = pstmt.execute();

        } catch (SQLException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                pstmt.close();
                manager.close();
            } catch (SQLException ex) {
                Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return flag;
    }
    
    private String getTitle(NodeFilter titleFilter, Parser parser) {
    	 String titleName = "";
         try {

             NodeList titleNodeList = (NodeList) parser.parse(titleFilter);
             for (int i = 0; i < titleNodeList.size(); i++) {
                 //HeadingTag title = (HeadingTag) titleNodeList.elementAt(i);
                 //titleName = title.getStringText();
            	 titleName = titleNodeList.elementAt(i).toPlainTextString();
             }

         } catch (ParserException ex) {
             Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
         }
         titleName = titleName.replace("\r\n", "");
         titleName = titleName.replace("\t", "");
         titleName = titleName.replace(" ", "");
         return titleName;
    }
    
    private String getzy(NodeFilter zyFilter, Parser parser)
    {
    	String newszy = null;
    	try{
    		NodeList zylist = (NodeList) parser.parse(zyFilter);
    		for (int i = 0; i < zylist.size(); i++) {
    			MetaTag zyTag = (MetaTag) zylist.elementAt(i);
    			newszy = zyTag.getMetaContent();
    		}
    	}catch (ParserException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
    	}
    	if(newszy.length() > 100)
    		newszy=newszy.substring(0,99).trim();
    	return newszy;
    }
    
    private String getNewsDate(NodeFilter dateFilter, Parser parser) {
        String newsDate = null;
        try {
            NodeList dateList = (NodeList) parser.parse(dateFilter);
            for (int i = 0; i < dateList.size(); i++) {
            	newsDate = dateList.elementAt(i).toPlainTextString();
            }
        } catch (ParserException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newsDate;
    }
    
    private String getNewsContent(NodeFilter newsContentFilter, Parser parser) {
        String content = null;
        StringBuilder builder = new StringBuilder();
        try {
            NodeList newsContentList = (NodeList) parser.parse(newsContentFilter);
            for (int i = 0; i < newsContentList.size(); i++) {
                Div newsContenTag = (Div) newsContentList.elementAt(i);
                builder = builder.append(newsContenTag.getStringText());
            }
            content = builder.toString();  //转换为String 类型。
            if (content != null) {
                parser.reset();
                parser = Parser.createParser(content, "gb2312");
                StringBean sb = new StringBean();
                sb.setCollapse(true);
                parser.visitAllNodesWith(sb);
                content = sb.getStrings();
//                String s = "\";} else{ document.getElementById('TurnAD444').innerHTML = \"\";} } showTurnAD444(intTurnAD444); }catch(e){}";
               
                //content = content.replaceAll("\\\".*[a-z].*\\}", "");
                content = content.replace("[我来说两句]", "");
                content = content.replace("\r\n", "</p>\n<p>");
                content = "<p>" + content + "</p>";
            } else {
               System.out.println("没有得到新闻内容！");
            }

        } catch (ParserException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return content;
    }

    /**
     * 根据提供的URL，获取此URL对应网页所有的纯文本信息，次方法得到的信息不是很纯，
     *常常会得到我们不想要的数据。不过如果你只是想得到某个URL 里的所有纯文本信息，该方法还是很好用的。
     * @param url 提供的URL链接
     * @return RL对应网页的纯文本信息
     * @throws ParserException
     * @deprecated 该方法被 getNewsContent()替代。
     */
    @Deprecated
    public String getText(String url) throws ParserException {
        StringBean sb = new StringBean();

        //设置不需要得到页面所包含的链接信息
        sb.setLinks(false);
        //设置将不间断空格由正规空格所替代
        sb.setReplaceNonBreakingSpaces(true);
        //设置将一序列空格由一个单一空格所代替
        sb.setCollapse(true);
        //传入要解析的URL
        sb.setURL(url);

        //返回解析后的网页纯文本信息
        return sb.getStrings();
    }

    public void parser(String url,int id) {
        try {
        	//FileDownLoader downLoader = new FileDownLoader();
    		//String urls = downLoader.downloadFile(url);
            parser = new Parser(url);
            NodeFilter titleFilter = new OrFilter(new TagNameFilter("h1"), new TagNameFilter("title"));
            NodeFilter zyFilter = new AndFilter(new TagNameFilter("meta"),new HasAttributeFilter("name", "description"));
            
            OrFilter contentFilter1 = new OrFilter(new HasAttributeFilter("itemprop", "articleBody"), new HasAttributeFilter("id", "artibody"));
            OrFilter contentFilter2 = new OrFilter(contentFilter1, new HasAttributeFilter("id", "endText"));
            OrFilter contentFilter3 = new OrFilter(contentFilter2, new HasAttributeFilter("class", "left_zw"));
            OrFilter contentFilter4 = new OrFilter(contentFilter3, new HasAttributeFilter("id", "content"));
            NodeFilter contentFilter = new AndFilter(new TagNameFilter("div"), contentFilter4);
            
            OrFilter newsdateFilter1 = new OrFilter(new HasAttributeFilter("class", "time"), new HasAttributeFilter("id", "pub_date"));
            OrFilter newsdateFilter3 = new OrFilter(newsdateFilter1, new HasAttributeFilter("id", "ptime"));
            OrFilter newsdateFilter2 = new OrFilter(new TagNameFilter("Div"), new TagNameFilter("span"));
            NodeFilter newsdateFilter = new AndFilter(newsdateFilter2, newsdateFilter3);
            String newsTitle = getTitle(titleFilter, parser);
            if(newsTitle != null)
            	System.out.println("标题\n"+newsTitle);
            parser.reset();   //记得每次用完parser后，要重置一次parser。要不然就得不到我们想要的内容了。
            
            String newszy = getzy(zyFilter, parser);
            if(newszy != null)
            	System.out.println("摘要\n"+newszy);
            parser.reset();
            
            String newsContent = getNewsContent(contentFilter, parser);
            if(newsContent != null)
            	System.out.println("正文\n"+newsContent);   //输出新闻的内容，查看是否符合要求
            parser.reset();
            
            String newsDate = getNewsDate(newsdateFilter, parser);
            
            if(newsDate == null)
            {
            	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            	newsDate = df.format(new Date());// new Date()为获取当前系统时间
            }
            else if(newsDate.length() > 20){
            	newszy=newszy.substring(0,19).trim();
            }
            System.out.println("日期\n"+newsDate);
            parser.reset();

            //先设置新闻对象，让新闻对象里有新闻内容。
            if(newsTitle != null)
            {
            	setNews(newsTitle,newszy,newsContent, newsDate, url);
            	this.newsToDataBase(id);
            }
            
        } catch (ParserException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //单个文件测试网页
    public static void main(String[] args) {
    	newsparser news = new newsparser();
        news.parser("http://sports.163.com/13/1220/08/9GHC80OC00052UUC.html",5);   
    }
}
