package eu.larkc.csparql.core.reasoner;

/**
 * @author 杨帆玉
 * @date 2019/12/3 4:13 下午
 */
public enum IndexType {
    S__(1), SP_(2), S_O(3), _P_(4), _PO(5), __O(6);

    int type;

    IndexType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
