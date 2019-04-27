package pattern;

public class Main {

    public static void main(String[] args) {
       SportSubject basketballSubject = new SportSubject("篮球运动");
        SportSubject soccerSubject = new SportSubject("足球运动");
        Subscriber user1 = new BaseSubscriber("tom");
        Subscriber user2 = new BaseSubscriber("李梅");
        user1.subscribe(basketballSubject);//tom订阅了篮球主题
        user2.subscribe(soccerSubject);//李梅订阅了足球主题
        user2.subscribe(basketballSubject);//李梅订阅了篮球主题
        basketballSubject.publish(new Article("小试牛刀？詹姆斯在其大儿子比赛期间进场练习投篮"));
        soccerSubject.publish(new Article("|武磊留洋| 鬼魅跑位+侧身凌空斩，武磊打入西甲第二球！！"));

    }
}
