package pattern;

import java.util.List;

/**
 * Created by 82138 on 2019/4/27.
 */
public interface Subscriber {
    //得到所有主题
    List getSubjects();
    //是否订阅主题
    boolean isSubscribed(Subject subject);
    //订阅主题
    void subscribe(Subject subject);
    //取消订阅
    void cancle(Subject subject);
}
