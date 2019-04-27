package pattern;

import java.util.Observable;
import java.util.Observer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by 82138 on 2019/4/27.
 */
public class BaseSubscriber implements Observer,Subscriber{
    //用户订阅de主题
    private List<Subject> subjects = new CopyOnWriteArrayList<>();
    private  String userName;
    public BaseSubscriber(String name){
        userName = name;
    }
    @Override
    public void update(Observable o, Object arg) {
        if(o instanceof BaseSubject){
            BaseSubject s = (BaseSubject) o;
            System.out.println("尊敬的用户 【"+userName+"】 你好,您的主题【"+ s.getName()+"】已经更新.");
            if(!subjects.contains(o)) subjects.add(s);
            ((List)arg).forEach(System.out::println);
        }
    }

    @Override
    public List getSubjects() {
        return subjects;
    }

    @Override
    public boolean isSubscribed(Subject subject) {
        return subjects.contains(subject);
    }

    @Override
    public void subscribe(Subject subject) {
        subjects.add(subject);
        ((BaseSubject)subject).addObserver(this);//用户订阅的同时，将该用户加入到主题的用户列表中

    }
    public void cancle(Subject subject){
        subjects.remove(subject);
        ((BaseSubject)subject).deleteObserver(this);//用户取消订阅的同时，将该用户从主题的用户列表中移除
    }
}
