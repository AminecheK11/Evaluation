package ma.projet.beans;

import javax.persistence.*;
import org.hibernate.annotations.Check;
import java.util.Date;

@Entity
@Table(name = "mariage", uniqueConstraints = @UniqueConstraint(
        name = "uk_mariage_couple_debut", columnNames = {"homme_id", "femme_id", "date_debut"}))
@Check(constraints = "nbr_enfant >= 0 and (date_fin is null or date_fin >= date_debut)")
public class Mariage {
    // Cle technique choisie pour permettre le remariage d'un meme couple a une autre date.
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "homme_id", nullable = false)
    private Homme homme;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "femme_id", nullable = false)
    private Femme femme;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_debut", nullable = false)
    private Date dateDebut;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_fin")
    private Date dateFin;
    @Column(name = "nbr_enfant", nullable = false)
    private int nbrEnfant;

    public Mariage() { }
    public Mariage(Homme homme, Femme femme, Date debut, Date fin, int enfants) {
        if (homme == null || femme == null) throw new IllegalArgumentException("Homme et femme obligatoires.");
        this.homme = homme; this.femme = femme;
        setDates(debut, fin); setNbrEnfant(enfants);
    }
    public int getId() { return id; }
    public Homme getHomme() { return homme; }
    public Femme getFemme() { return femme; }
    public Date getDateDebut() { return new Date(dateDebut.getTime()); }
    public Date getDateFin() { return dateFin == null ? null : new Date(dateFin.getTime()); }
    public int getNbrEnfant() { return nbrEnfant; }
    public void setNbrEnfant(int enfants) {
        if (enfants < 0) throw new IllegalArgumentException("Le nombre d'enfants doit etre positif ou nul.");
        nbrEnfant = enfants;
    }
    public void setDates(Date debut, Date fin) {
        if (debut == null || (fin != null && debut.after(fin)))
            throw new IllegalArgumentException("Dates du mariage invalides.");
        dateDebut = new Date(debut.getTime());
        dateFin = fin == null ? null : new Date(fin.getTime());
    }
    public void setDateDebut(Date debut) { setDates(debut, dateFin); }
    public void setDateFin(Date fin) { setDates(dateDebut, fin); }
}
