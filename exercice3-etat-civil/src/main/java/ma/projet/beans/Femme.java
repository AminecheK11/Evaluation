package ma.projet.beans;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "femme")
@PrimaryKeyJoinColumn(name = "id")
@SqlResultSetMapping(name="nombreEnfants", columns=@ColumnResult(name="total_enfants", type=Long.class))
@NamedNativeQuery(name = "Femme.nombreEnfantsEntreDates",
        query = """
                SELECT COALESCE(SUM(nbr_enfant), 0) AS total_enfants FROM mariage
                WHERE femme_id = :femmeId AND date_debut <= :fin
                AND (date_fin IS NULL OR date_fin >= :debut)
                """, resultSetMapping = "nombreEnfants")
@NamedQuery(name = "Femme.marieesAuMoinsDeuxFois",
        query = """
                select f from Femme f
                where (select count(m.id) from Mariage m where m.femme = f) >= 2
                order by f.id
                """)
public class Femme extends Personne {
    @OneToMany(mappedBy = "femme")
    private List<Mariage> mariages = new ArrayList<>();

    public Femme() { }
    public Femme(String nom, String prenom, String telephone, String adresse, Date naissance) {
        super(nom, prenom, telephone, adresse, naissance);
    }
    public List<Mariage> getMariages() { return mariages; }
}
