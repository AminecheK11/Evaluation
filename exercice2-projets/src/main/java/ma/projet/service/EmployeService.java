package ma.projet.service;

import ma.projet.classes.*;
import java.util.List;

public class EmployeService extends AbstractService<Employe> {
    public EmployeService() { super(Employe.class); }

    public List<Tache> findTachesRealisees(int employeId) {
        return inTransaction(session -> session.createQuery("""
                select distinct t from EmployeTache et
                join et.tache t join fetch t.projet p join fetch p.chefProjet
                where et.employe.id = :id and et.dateFinReelle is not null
                order by t.id
                """, Tache.class).setParameter("id", employeId).getResultList());
    }

    public List<Projet> findProjetsGeres(int employeId) {
        return inTransaction(session -> session.createQuery("""
                select p from Projet p join fetch p.chefProjet
                where p.chefProjet.id = :id order by p.id
                """, Projet.class).setParameter("id", employeId).getResultList());
    }
}
