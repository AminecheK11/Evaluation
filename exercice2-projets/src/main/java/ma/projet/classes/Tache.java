package ma.projet.classes;

import javax.persistence.*;
import ma.projet.util.DatesProjet;
import org.hibernate.annotations.Check;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "tache")
@Check(constraints = "date_fin >= date_debut and prix >= 0")
@NamedQuery(name = "Tache.prixSuperieur1000",
        query = "select t from Tache t join fetch t.projet p join fetch p.chefProjet where t.prix > 1000 order by t.id")
public class Tache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String nom;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_debut", nullable = false)
    private Date dateDebut;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_fin", nullable = false)
    private Date dateFin;

    @Column(nullable = false)
    private double prix;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;
    @OneToMany(mappedBy = "tache", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeTache> affectations = new ArrayList<>();

    public Tache() { }
    public Tache(String nom, Date dateDebut, Date dateFin, double prix, Projet projet) {
        this.nom = nom;
        setDatesPlanifiees(dateDebut, dateFin);
        setPrix(prix);
        setProjet(projet);
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Date getDateDebut() { return DatesProjet.copie(dateDebut); }
    public Date getDateFin() { return DatesProjet.copie(dateFin); }
    public void setDatesPlanifiees(Date debut, Date fin) {
        DatesProjet.verifierPlanification(debut, fin);
        this.dateDebut = DatesProjet.copie(debut);
        this.dateFin = DatesProjet.copie(fin);
    }
    public void setDateDebut(Date debut) {
        if (debut == null || (dateFin != null && debut.after(dateFin))) {
            throw new IllegalArgumentException("Date de debut invalide.");
        }
        this.dateDebut = DatesProjet.copie(debut);
    }
    public void setDateFin(Date fin) {
        if (fin == null || (dateDebut != null && dateDebut.after(fin))) {
            throw new IllegalArgumentException("Date de fin invalide.");
        }
        this.dateFin = DatesProjet.copie(fin);
    }
    public double getPrix() { return prix; }
    public void setPrix(double prix) {
        if (!Double.isFinite(prix) || prix < 0) throw new IllegalArgumentException("Prix invalide.");
        this.prix = prix;
    }
    public Projet getProjet() { return projet; }
    public void setProjet(Projet projet) {
        if (projet == null) throw new IllegalArgumentException("Projet obligatoire.");
        this.projet = projet;
    }
    public List<EmployeTache> getAffectations() { return affectations; }
    @Override
    public String toString() { return id + " | " + nom + " | " + prix + " DH"; }
}
