package ma.projet.classes;

import javax.persistence.*;
import ma.projet.util.DatesProjet;
import org.hibernate.annotations.Check;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "projet")
@Check(constraints = "date_fin >= date_debut")
public class Projet {
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chef_projet_id", nullable = false)
    private Employe chefProjet;
    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tache> taches = new ArrayList<>();

    public Projet() { }
    public Projet(String nom, Date dateDebut, Date dateFin, Employe chefProjet) {
        this.nom = nom;
        setDatesPlanifiees(dateDebut, dateFin);
        setChefProjet(chefProjet);
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
    public Employe getChefProjet() { return chefProjet; }
    public void setChefProjet(Employe chefProjet) {
        if (chefProjet == null) throw new IllegalArgumentException("Chef de projet obligatoire.");
        this.chefProjet = chefProjet;
    }
    public List<Tache> getTaches() { return taches; }
    @Override
    public String toString() { return id + " | " + nom; }
}
