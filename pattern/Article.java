package pattern;

import java.time.LocalDateTime;

/**
 * Created by 82138 on 2019/4/27.
 */
public class Article implements Comparable<Article>{
    private String name;
    private LocalDateTime time;
    public Article(String name){
        this.name=name;
        time = LocalDateTime.now();
    }
    @Override
    public int compareTo(Article o) {
        return time.compareTo(o.getTime());
    }


    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
