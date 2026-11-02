package ma.projet.service;

import ma.projet.beans.*;
import java.util.List;

public class MariageService extends AbstractService<Mariage> {
    public MariageService() { super(Mariage.class); }
    @Override public Mariage findById(java.io.Serializable id) {
        return transaction(s -> s.createQuery("""
                select m from Mariage m join fetch m.homme join fetch m.femme where m.id = :id
                """, Mariage.class).setParameter("id", id).uniqueResult());
    }
    @Override public List<Mariage> findAll() {
        return transaction(s -> s.createQuery("""
                select m from Mariage m join fetch m.homme join fetch m.femme order by m.id
                """, Mariage.class).getResultList());
    }
    public List<Mariage> findByHomme(int hommeId) {
        return transaction(s -> s.createQuery("""
                select m from Mariage m join fetch m.homme join fetch m.femme
                where m.homme.id = :id order by m.dateDebut, m.id
                """, Mariage.class).setParameter("id", hommeId).getResultList());
    }
}
