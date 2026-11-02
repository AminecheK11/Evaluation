package ma.projet.service;

import ma.projet.classes.*;

public class EmployeTacheService extends AbstractService<EmployeTache> {
    public EmployeTacheService() { super(EmployeTache.class); }

    @Override
    public boolean create(EmployeTache affectation) {
        return inTransaction(session -> {
            Employe employe = session.get(Employe.class, affectation.getId().getEmployeId());
            Tache tache = session.get(Tache.class, affectation.getId().getTacheId());
            if (employe == null || tache == null) {
                throw new IllegalArgumentException("Enregistrer l'employe et la tache avant l'affectation.");
            }
            session.persist(new EmployeTache(employe, tache,
                    affectation.getDateDebutReelle(), affectation.getDateFinReelle()));
            return true;
        });
    }

    @Override
    public EmployeTache findById(java.io.Serializable id) {
        return inTransaction(session -> session.createQuery("""
                select et from EmployeTache et join fetch et.employe
                join fetch et.tache t join fetch t.projet p join fetch p.chefProjet
                where et.id = :id
                """, EmployeTache.class).setParameter("id", id).uniqueResult());
    }
}
