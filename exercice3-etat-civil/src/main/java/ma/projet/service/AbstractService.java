package ma.projet.service;

import ma.projet.dao.IDao;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractService<T> implements IDao<T> {
    private final Class<T> type;
    protected AbstractService(Class<T> type) { this.type = type; }
    protected <R> R transaction(Function<Session, R> action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                R result = action.apply(session); tx.commit(); return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    try { tx.rollback(); } catch (RuntimeException rollback) { e.addSuppressed(rollback); }
                }
                throw e;
            }
        }
    }
    @Override public boolean create(T entity) {
        return transaction(s -> { s.persist(entity); return true; });
    }
    @Override public boolean update(T entity) {
        return transaction(s -> {
            java.io.Serializable id = (java.io.Serializable) s.getSessionFactory().getPersistenceUnitUtil().getIdentifier(entity);
            if (id == null || s.get(type, id) == null) return false;
            s.merge(entity); return true;
        });
    }
    @Override public boolean delete(T entity) {
        return transaction(s -> {
            java.io.Serializable id = (java.io.Serializable) s.getSessionFactory().getPersistenceUnitUtil().getIdentifier(entity);
            T managed = id == null ? null : s.get(type, id);
            if (managed == null) return false;
            s.remove(managed); return true;
        });
    }
    @Override public T findById(java.io.Serializable id) { return transaction(s -> s.get(type, id)); }
    @Override public List<T> findAll() {
        return transaction(s -> s.createQuery("from " + type.getSimpleName() + " e order by e.id", type).getResultList());
    }
}
