package search.common.entity.help;

import search.common.entity.bizesinterface.IndexObjEntity;

import java.util.Collection;

/**
 * Created by soledede.weng on 2016/8/24.
 */
public class IndexHelpEntity {
    private String indexName;
    private String typeName;
    private Collection<IndexObjEntity> data;
    private String typeChoose;

    public IndexHelpEntity() {
    }

    public IndexHelpEntity(String indexName, String typeName, Collection<IndexObjEntity> data, String typeChoose) {
        this.indexName = indexName;
        this.typeName = typeName;
        this.data = data;
        this.typeChoose = typeChoose;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Collection<IndexObjEntity> getData() {
        return data;
    }

    public void setData(Collection<IndexObjEntity> data) {
        this.data = data;
    }

    public String getTypeChoose() {
        return typeChoose;
    }

    public void setTypeChoose(String typeChoose) {
        this.typeChoose = typeChoose;
    }
}
