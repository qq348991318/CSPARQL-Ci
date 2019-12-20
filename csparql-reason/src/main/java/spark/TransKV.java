package spark;


import com.triple.common.RDFTriple;

import java.util.Objects;

/**
 * @author 杨帆玉
 * @date 2019/11/12 12:27 下午
 */
public class TransKV {
    public String key;
    public RDFTriple value;

    public TransKV() {
    }

    public TransKV(String key, RDFTriple value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public RDFTriple getValue() {
        return value;
    }

    public void setValue(RDFTriple value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransKV transKV = (TransKV) o;
        return Objects.equals(key, transKV.key) &&
                Objects.equals(value, transKV.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
