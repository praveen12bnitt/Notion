package edu.mayo.qia.pacs.components;

import java.io.File;

import javax.transaction.Transactional;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.Audit;

@Transactional
@Component
public class PoolContainerSupport {

	@Autowired
	private SessionFactory sessionFactory;
	
	

	public Study saveDicomMetadataToDB(ProcessCache cache, DicomObject tags, File relativePath, Pool pool, File poolDirectory) {

		Session session = sessionFactory.getCurrentSession();
		try {
			pool = (Pool) session.merge(pool);
			
			Query query;

			Study study = cache.studies.get(tags.getString(Tag.StudyInstanceUID));
			if (study == null) {

				query = session.createQuery("from Study where PoolKey = :poolkey and StudyInstanceUID = :suid");
				query.setInteger("poolkey", pool.poolKey);
				query.setString("suid", tags.getString(Tag.StudyInstanceUID));
				study = (Study) query.uniqueResult();

				if (study == null) {
					study = new Study(tags);
					study.pool = pool;
					// Log when we get a new study
					Audit.log(pool.toString(), "create_study", tags);
				} else {
					study.update(tags);
					Audit.log(pool.toString(), "update_study", tags);
				}
				session.saveOrUpdate(study);
				cache.studies.put(tags.getString(Tag.StudyInstanceUID), study);
				
				
			}
			
			study = (Study) session.merge(study);

			Series series = cache.series.get(tags.getString(Tag.SeriesInstanceUID));
			if (series == null) {
				// Find the Series
				query = session.createQuery("from Series where StudyKey = :studykey and SeriesInstanceUID = :suid");
				query.setInteger("studykey", study.StudyKey);
				query.setString("suid", tags.getString(Tag.SeriesInstanceUID));
				series = (Series) query.uniqueResult();
				if (series == null) {
					series = new Series(tags);
					series.study = study;
					// Log when we get a new study
					Audit.log(pool.toString(), "create_series", tags);
				} else {
					series.update(tags);
				}
				session.saveOrUpdate(series);
				cache.series.put(tags.getString(Tag.SeriesInstanceUID), series);
				
			}
			
			series = (Series) session.merge(series);

			// Find the Instance
			query = session.createQuery("from Instance where SeriesKey = :serieskey and SOPInstanceUID = :suid").setInteger("serieskey", series.SeriesKey);
			query.setString("suid", tags.getString(Tag.SOPInstanceUID));
			Instance instance = (Instance) query.uniqueResult();
			if (instance == null) {
				instance = new Instance(tags, relativePath.getPath());
				instance.series = series;
			} else {
				instance.update(tags);
				// Update the file path, delete the existing file
				File existingFile = new File(poolDirectory, instance.FilePath);
				instance.FilePath = relativePath.getPath();
				existingFile.delete();
			}
			return study;
		} finally {
			
		}
		

		

		
	}
}
