package ma.projet.classes;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employe")
public class Employe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String nom;
    @Column(nullable = false)
    private String prenom;
    @Column(nullable = false)
    private String telephone;

    @OneToMany(mappedBy = "chefProjet")
    private List<Projet> projetsGeres = new ArrayList<>();
    @OneToMany(mappedBy = "employe")
    private List<EmployeTache> affectations = new ArrayList<>();

    public Employe() { }
    public Employe(String nom, String prenom, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public List<Projet> getProjetsGeres() { return projetsGeres; }
    public List<EmployeTache> getAffectations() { return affectations; }
    @Override
    public String toString() { return id + " | " + nom + " " + prenom + " | " + telephone; }
}
