package ma.projet.service;

import ma.projet.dao.IDao;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.function.Function;

/** Chaque operation ouvre sa session, valide sa transaction et ferme la session. */
public abstract class AbstractService<T> implements IDao<T> {
    private final Class<T> entityClass;

    protected AbstractService(Class<T> entityClass) { this.entityClass = entityClass; }

    protected <R> R inTransaction(Function<Session, R> work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                R result = work.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    try { transaction.rollback(); } catch (RuntimeException rollback) { e.addSuppressed(rollback); }
                }
                throw e;
            }
        }
    }

    @Override
    public boolean create(T entity) {
        return inTransaction(session -> { session.persist(entity); return true; });
    }

    @Override
    public boolean update(T entity) {
        return inTransaction(session -> {
            java.io.Serializable id = (java.io.Serializable) session.getSessionFactory().getPersistenceUnitUtil().getIdentifier(entity);
            if (id == null || session.get(entityClass, id) == null) return false;
            session.merge(entity);
            return true;
        });
    }

    @Override
    public boolean delete(T entity) {
        return inTransaction(session -> {
            java.io.Serializable id = (java.io.Serializable) session.getSessionFactory().getPersistenceUnitUtil().getIdentifier(entity);
            T managed = id == null ? null : session.get(entityClass, id);
            if (managed == null) return false;
            session.remove(managed);
            return true;
        });
    }

    @Override
    public T findById(java.io.Serializable id) {
        return inTransaction(session -> session.get(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        return inTransaction(session -> session.createQuery(
                "from " + entityClass.getSimpleName(), entityClass).getResultList());
    }
}
