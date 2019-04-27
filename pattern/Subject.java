package pattern; /**
 * Created by 82138 on 2019/4/27.
 */

import java.util.Map;


public interface Subject {
    //订阅者数量
    int getSubscribersCount();

    //发布内容
    void publish(Article[] articles);
    //该主题得所有文章
    Map getArticles();
}
