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
	private Parser parser = null;   //���ڷ�����ҳ�ķ�������
    //private List newsList = new ArrayList();    //�ݴ����ŵ�List��
    private NewsBean bean = new NewsBean();
    private ConnectionManager manager = null;    //���ݿ����ӹ�������
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

        //����һ���߳�����ִ�н����Ų��뵽���ݿ��С�
        Thread thread = new Thread(new Runnable() {

            public void run() {
                boolean sucess = saveToDB(bean,id);
                if (sucess != false) {
                    System.out.println("��������ʧ��");
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
        if (titleLength.length() > 50) {  //����̫�������Ų�Ҫ��
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
            pstmt.setString(6, "������");
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
            content = builder.toString();  //ת��ΪString ���͡�
            if (content != null) {
                parser.reset();
                parser = Parser.createParser(content, "gb2312");
                StringBean sb = new StringBean();
                sb.setCollapse(true);
                parser.visitAllNodesWith(sb);
                content = sb.getStrings();
//                String s = "\";} else{ document.getElementById('TurnAD444').innerHTML = \"\";} } showTurnAD444(intTurnAD444); }catch(e){}";
               
                //content = content.replaceAll("\\\".*[a-z].*\\}", "");
                content = content.replace("[����˵����]", "");
                content = content.replace("\r\n", "</p>\n<p>");
                content = "<p>" + content + "</p>";
            } else {
               System.out.println("û�еõ��������ݣ�");
            }

        } catch (ParserException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return content;
    }

    /**
     * �����ṩ��URL����ȡ��URL��Ӧ��ҳ���еĴ��ı���Ϣ���η����õ�����Ϣ���Ǻܴ���
     *������õ����ǲ���Ҫ�����ݡ����������ֻ����õ�ĳ��URL ������д��ı���Ϣ���÷������Ǻܺ��õġ�
     * @param url �ṩ��URL����
     * @return RL��Ӧ��ҳ�Ĵ��ı���Ϣ
     * @throws ParserException
     * @deprecated �÷����� getNewsContent()�����
     */
    @Deprecated
    public String getText(String url) throws ParserException {
        StringBean sb = new StringBean();

        //���ò���Ҫ�õ�ҳ����������������Ϣ
        sb.setLinks(false);
        //���ý�����Ͽո�������ո������
        sb.setReplaceNonBreakingSpaces(true);
        //���ý�һ���пո���һ����һ�ո�������
        sb.setCollapse(true);
        //����Ҫ������URL
        sb.setURL(url);

        //���ؽ��������ҳ���ı���Ϣ
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
            	System.out.println("����\n"+newsTitle);
            parser.reset();   //�ǵ�ÿ������parser��Ҫ����һ��parser��Ҫ��Ȼ�͵ò���������Ҫ�������ˡ�
            
            String newszy = getzy(zyFilter, parser);
            if(newszy != null)
            	System.out.println("ժҪ\n"+newszy);
            parser.reset();
            
            String newsContent = getNewsContent(contentFilter, parser);
            if(newsContent != null)
            	System.out.println("����\n"+newsContent);   //������ŵ����ݣ��鿴�Ƿ����Ҫ��
            parser.reset();
            
            String newsDate = getNewsDate(newsdateFilter, parser);
            
            if(newsDate == null)
            {
            	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//�������ڸ�ʽ
            	newsDate = df.format(new Date());// new Date()Ϊ��ȡ��ǰϵͳʱ��
            }
            else if(newsDate.length() > 20){
            	newszy=newszy.substring(0,19).trim();
            }
            System.out.println("����\n"+newsDate);
            parser.reset();

            //���������Ŷ��������Ŷ��������������ݡ�
            if(newsTitle != null)
            {
            	setNews(newsTitle,newszy,newsContent, newsDate, url);
            	this.newsToDataBase(id);
            }
            
        } catch (ParserException ex) {
            Logger.getLogger(newsparser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //�����ļ�������ҳ
    public static void main(String[] args) {
    	newsparser news = new newsparser();
        news.parser("http://sports.163.com/13/1220/08/9GHC80OC00052UUC.html",5);   
    }
}
