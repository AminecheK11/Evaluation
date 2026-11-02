package ma.projet.service;

import ma.projet.beans.*;
import ma.projet.util.Periode;
import javax.persistence.criteria.*;
import java.io.PrintStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

public class HommeService extends AbstractService<Homme> {
    public HommeService() { super(Homme.class); }

    /** Chevauchement de la periode, bornes inclusives ; une epouse apparait une seule fois. */
    public List<Femme> findEpousesEntreDates(int hommeId, java.util.Date debut, java.util.Date fin) {
        Periode.verifier(debut, fin);
        return transaction(s -> s.createQuery("""
                select distinct f from Mariage m join m.femme f
                where m.homme.id = :id and m.dateDebut <= :fin
                and (m.dateFin is null or m.dateFin >= :debut) order by f.id
                """, Femme.class).setParameter("id", hommeId)
                .setParameter("debut", debut, javax.persistence.TemporalType.DATE)
                .setParameter("fin", fin, javax.persistence.TemporalType.DATE).getResultList());
    }

    /**
     * API Criteria exclusivement : cherche exactement quatre epouses distinctes simultanees.
     * Une union se termine en fin de journee (bornes inclusives).
     * Le nombre actif ne change qu'aux debuts et aux lendemains des fins.
     */
    public List<Homme> findHommesMariesAQuatreFemmesEntreDates(java.util.Date debut, java.util.Date fin) {
        Periode.verifier(debut, fin);
        return transaction(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            Set<LocalDate> jours = new TreeSet<>();
            LocalDate premier = new Date(debut.getTime()).toLocalDate();
            LocalDate dernier = new Date(fin.getTime()).toLocalDate();
            jours.add(premier);
            for (String champ : List.of("dateDebut", "dateFin")) {
                CriteriaQuery<java.util.Date> evenements = cb.createQuery(java.util.Date.class);
                Root<Mariage> m = evenements.from(Mariage.class);
                evenements.select(m.<java.util.Date>get(champ)).distinct(true)
                        .where(cb.between(m.<java.util.Date>get(champ), Date.valueOf(premier), Date.valueOf(dernier)));
                for (java.util.Date valeur : s.createQuery(evenements).getResultList()) {
                    LocalDate jour = new Date(valeur.getTime()).toLocalDate();
                    if (champ.equals("dateFin")) {
                        if (!jour.isBefore(dernier)) continue;
                        jour = jour.plusDays(1);
                    }
                    jours.add(jour);
                }
            }
            Set<Integer> ids = new TreeSet<>();
            for (LocalDate jour : jours) {
                CriteriaQuery<Integer> groupes = cb.createQuery(Integer.class);
                Root<Mariage> m = groupes.from(Mariage.class);
                Path<Integer> hommeId = m.get("homme").get("id");
                groupes.select(hommeId).where(
                        cb.lessThanOrEqualTo(m.<java.util.Date>get("dateDebut"), Date.valueOf(jour)),
                        cb.or(cb.isNull(m.get("dateFin")),
                                cb.greaterThanOrEqualTo(m.<java.util.Date>get("dateFin"), Date.valueOf(jour))))
                        .groupBy(hommeId).having(cb.equal(cb.countDistinct(m.get("femme").get("id")), 4L));
                ids.addAll(s.createQuery(groupes).getResultList());
            }
            if (ids.isEmpty()) return List.of();
            CriteriaQuery<Homme> resultat = cb.createQuery(Homme.class);
            Root<Homme> h = resultat.from(Homme.class);
            resultat.select(h).where(h.get("id").in(ids)).orderBy(cb.asc(h.get("id")));
            return s.createQuery(resultat).getResultList();
        });
    }
    public long compterHommesMariesAQuatreFemmesEntreDates(java.util.Date debut, java.util.Date fin) {
        return findHommesMariesAQuatreFemmesEntreDates(debut, fin).size();
    }

    public void afficherEpousesEntreDates(int hommeId, java.util.Date debut, java.util.Date fin, PrintStream out) {
        out.println("Épouses de l'homme " + hommeId + " pendant la période :");
        findEpousesEntreDates(hommeId, debut, fin).forEach(out::println);
    }
    /** En cours = dateFin absente ; echoue = dateFin renseignee, selon l'enonce. */
    public void afficherMariages(int hommeId, PrintStream out) {
        Homme h = findById(hommeId);
        if (h == null) throw new IllegalArgumentException("Homme inexistant : " + hommeId);
        List<Mariage> liste = new MariageService().findByHomme(hommeId);
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        out.println("Mariages de " + h.getNom() + " " + h.getPrenom() + " (id " + h.getId() + ")");
        for (boolean enCours : new boolean[]{true, false}) {
            out.println(enCours ? "\nMariages en cours :" : "\nMariages échoués :");
            out.printf("%-28s %-14s %-14s %s%n", "Femme", "Date début", "Date fin", "Enfants");
            boolean present = false;
            for (Mariage m : liste) {
                if ((m.getDateFin() == null) != enCours) continue;
                present = true;
                out.printf("%-28s %-14s %-14s %d%n",
                        m.getFemme().getNom() + " " + m.getFemme().getPrenom(),
                        date.format(m.getDateDebut()), m.getDateFin() == null ? "—" : date.format(m.getDateFin()),
                        m.getNbrEnfant());
            }
            if (!present) out.println("Aucun mariage.");
        }
    }
}
