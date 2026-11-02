package ma.projet.classes;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "commande")
public class Commande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_commande", nullable = false)
    private Date date;

    @OneToMany(mappedBy = "commande")
    private List<LigneCommandeProduit> lignesCommande = new ArrayList<>();

    public Commande() { }
    public Commande(Date date) { setDate(date); }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Date getDate() { return date == null ? null : new Date(date.getTime()); }
    public void setDate(Date date) {
        if (date == null) throw new IllegalArgumentException("Date obligatoire.");
        this.date = new Date(date.getTime());
    }
    public List<LigneCommandeProduit> getLignesCommande() { return lignesCommande; }
}
