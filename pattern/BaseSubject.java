package pattern;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 82138 on 2019/4/27.
 */
public abstract class BaseSubject extends Observable implements Subject{


    protected String name;
    private ConcurrentHashMap<String,Article> articles = new ConcurrentHashMap<>(2>>10);


    @Override
    public int getSubscribersCount() {
        return super.countObservers();
    }



    @Override
    public void publish(Article[] updateArticles) {
        for(int i=0;i<updateArticles.length;i++){
            final Article article = updateArticles[i];
            articles.put(article.getName(),updateArticles[i] );
        }
        setChanged();
        notifyObservers(Arrays.asList(updateArticles) );//通知订阅者，java的Observable类的自带实现。


    }
    //重写，方便调用
    public void publish(Article updateArticle){
        publish(new Article[] {updateArticle});
    }


    public ConcurrentHashMap<String, Article> getArticles() {
        return articles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
