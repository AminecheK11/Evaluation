package ma.projet.service;

import ma.projet.classes.*;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProjetService extends AbstractService<Projet> {
    public ProjetService() { super(Projet.class); }

    public List<Tache> findTachesPlanifiees(int projetId) {
        return inTransaction(session -> session.createQuery("""
                select t from Tache t join fetch t.projet p join fetch p.chefProjet
                where p.id = :id order by t.id
                """, Tache.class).setParameter("id", projetId).getResultList());
    }

    /** Une ligne par affectation terminee pour conserver les dates de chaque employe. */
    public List<EmployeTache> findTachesRealisees(int projetId) {
        return inTransaction(session -> session.createQuery("""
                select et from EmployeTache et
                join fetch et.employe e join fetch et.tache t
                join fetch t.projet p join fetch p.chefProjet
                where p.id = :id and et.dateFinReelle is not null
                order by t.id, e.id
                """, EmployeTache.class).setParameter("id", projetId).getResultList());
    }

    /** Somme des prix des taches, independamment du nombre d'employes affectes. */
    public double calculerCoutGlobal(int projetId) {
        return inTransaction(session -> session.createQuery(
                "select coalesce(sum(t.prix), 0.0) from Tache t where t.projet.id = :id",
                Double.class).setParameter("id", projetId).getSingleResult());
    }

    public void afficherTachesRealisees(int projetId, PrintStream out) {
        Projet projet = findById(projetId);
        if (projet == null) throw new IllegalArgumentException("Projet inexistant : " + projetId);
        List<EmployeTache> affectations = findTachesRealisees(projetId);
        SimpleDateFormat dateLongue = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
        SimpleDateFormat dateCourte = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        out.println("Projet : " + projet.getId());
        out.println("Nom : " + projet.getNom());
        out.println("Date début : " + dateLongue.format(projet.getDateDebut()));
        out.println("\nListe des tâches réalisées :");
        out.printf("%-6s %-24s %-20s %-20s %s%n",
                "Num", "Nom", "Date Début Réelle", "Date Fin Réelle", "Employé");
        for (EmployeTache et : affectations) {
            out.printf("%-6d %-24s %-20s %-20s %s %s%n",
                    et.getTache().getId(), et.getTache().getNom(),
                    dateCourte.format(et.getDateDebutReelle()), dateCourte.format(et.getDateFinReelle()),
                    et.getEmploye().getNom(), et.getEmploye().getPrenom());
        }
    }
}
