package ma.projet.classes;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmployeTacheId implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "employe_id")
    private int employeId;
    @Column(name = "tache_id")
    private int tacheId;

    public EmployeTacheId() { }
    public EmployeTacheId(int employeId, int tacheId) {
        this.employeId = employeId;
        this.tacheId = tacheId;
    }
    public int getEmployeId() { return employeId; }
    public int getTacheId() { return tacheId; }
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof EmployeTacheId other)) return false;
        return employeId == other.employeId && tacheId == other.tacheId;
    }
    @Override
    public int hashCode() { return Objects.hash(employeId, tacheId); }
}
