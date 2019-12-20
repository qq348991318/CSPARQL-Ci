package sr4ld2014.streamer;

/**
 * @author 杨帆玉
 * @date 2019/10/24 3:42 下午
 */
public class RDFTriple {
    private String subject;
    private String predicate;
    private String objcet;

    public RDFTriple(String subject, String predicate, String objcet) {
        this.subject = subject;
        this.predicate = predicate;
        this.objcet = objcet;
    }

    public RDFTriple(String spo) {
        String[] strings = spo.split(" ");
        this.subject = strings[0];
        this.predicate = strings[1];
        this.objcet = strings[2];
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getObjcet() {
        return objcet;
    }

    public void setObjcet(String objcet) {
        this.objcet = objcet;
    }
}
