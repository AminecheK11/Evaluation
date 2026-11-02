package ma.projet.beans;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "homme")
@PrimaryKeyJoinColumn(name = "id")
public class Homme extends Personne {
    @OneToMany(mappedBy = "homme")
    private List<Mariage> mariages = new ArrayList<>();

    public Homme() { }
    public Homme(String nom, String prenom, String telephone, String adresse, Date naissance) {
        super(nom, prenom, telephone, adresse, naissance);
    }
    public List<Mariage> getMariages() { return mariages; }
}
