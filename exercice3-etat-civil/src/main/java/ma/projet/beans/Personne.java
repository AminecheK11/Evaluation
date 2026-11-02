package ma.projet.beans;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "personne")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Personne {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false) private String nom;
    @Column(nullable = false) private String prenom;
    @Column(nullable = false) private String telephone;
    @Column(nullable = false) private String adresse;
    @Temporal(TemporalType.DATE)
    @Column(name = "date_naissance", nullable = false)
    private Date dateNaissance;

    protected Personne() { }
    protected Personne(String nom, String prenom, String telephone, String adresse, Date dateNaissance) {
        this.nom = nom; this.prenom = prenom; this.telephone = telephone; this.adresse = adresse;
        setDateNaissance(dateNaissance);
    }
    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public Date getDateNaissance() { return new Date(dateNaissance.getTime()); }
    public void setDateNaissance(Date date) {
        if (date == null) throw new IllegalArgumentException("Date de naissance obligatoire.");
        dateNaissance = new Date(date.getTime());
    }
    @Override public String toString() {
        return id + " | " + nom + " " + prenom + " | " + new java.sql.Date(dateNaissance.getTime())
                + " | " + telephone + " | " + adresse;
    }
}
