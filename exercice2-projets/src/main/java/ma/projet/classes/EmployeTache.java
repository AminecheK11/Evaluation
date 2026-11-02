package ma.projet.classes;

import javax.persistence.*;
import ma.projet.util.DatesProjet;
import org.hibernate.annotations.Check;
import java.util.Date;

@Entity
@Table(name = "employe_tache")
@Check(constraints = "date_fin_reelle is null or (date_debut_reelle is not null and date_fin_reelle >= date_debut_reelle)")
public class EmployeTache {
    @EmbeddedId
    private EmployeTacheId id = new EmployeTacheId();
    @MapsId("employeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;
    @MapsId("tacheId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tache_id", nullable = false)
    private Tache tache;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_debut_reelle")
    private Date dateDebutReelle;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_fin_reelle")
    private Date dateFinReelle;

    public EmployeTache() { }
    public EmployeTache(Employe employe, Tache tache, Date debut, Date fin) {
        if (employe == null || tache == null) throw new IllegalArgumentException("Employe et tache obligatoires.");
        this.employe = employe;
        this.tache = tache;
        this.id = new EmployeTacheId(employe.getId(), tache.getId());
        setDatesReelles(debut, fin);
    }
    public EmployeTacheId getId() { return id; }
    public Employe getEmploye() { return employe; }
    public Tache getTache() { return tache; }
    public Date getDateDebutReelle() { return DatesProjet.copie(dateDebutReelle); }
    public Date getDateFinReelle() { return DatesProjet.copie(dateFinReelle); }
    public void setDatesReelles(Date debut, Date fin) {
        DatesProjet.verifierRealisation(debut, fin);
        dateDebutReelle = DatesProjet.copie(debut);
        dateFinReelle = DatesProjet.copie(fin);
    }
    public void setDateDebutReelle(Date debut) { setDatesReelles(debut, dateFinReelle); }
    public void setDateFinReelle(Date fin) { setDatesReelles(dateDebutReelle, fin); }
}
