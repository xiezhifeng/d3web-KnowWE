package de.d3web.we.core.knowledgeService;

import de.d3web.caserepository.addons.fus.FUSConfigurationReader;
import de.d3web.caserepository.addons.fus.FUSConfigurationWriter;
import de.d3web.caserepository.addons.fus.SimpleTextFUSsReader;
import de.d3web.caserepository.addons.fus.SimpleTextFUSsWriter;
import de.d3web.caserepository.addons.shared.AppliedQSetsReader;
import de.d3web.caserepository.addons.shared.AppliedQSetsWriter;
import de.d3web.caserepository.addons.shared.ConfigReader;
import de.d3web.caserepository.addons.shared.ConfigWriter;
import de.d3web.caserepository.addons.train.sax.AdditionalTrainDataSAXReader;
import de.d3web.caserepository.addons.train.sax.ContentsSAXReader;
import de.d3web.caserepository.addons.train.sax.ExaminationBlocksSAXReader;
import de.d3web.caserepository.addons.train.sax.MultimediaSAXReader;
import de.d3web.caserepository.addons.train.sax.MultimediaSimpleQuestionsSAXReader;
import de.d3web.caserepository.addons.train.sax.TemplateSessionReader;
import de.d3web.caserepository.addons.train.writer.AdditionalTrainDataWriter;
import de.d3web.caserepository.addons.train.writer.ContentsWriter;
import de.d3web.caserepository.addons.train.writer.ExaminationBlocksWriter;
import de.d3web.caserepository.addons.train.writer.MultimediaSimpleQuestionsWriter;
import de.d3web.caserepository.addons.train.writer.MultimediaWriter;
import de.d3web.caserepository.addons.train.writer.TemplateSessionWriter;
import de.d3web.persistence.DelegatePersistenceHandlerInitializer;
import de.d3web.persistence.xml.CaseRepositoryHandler;
import de.d3web.persistence.xml.PersistenceManager;
import de.d3web.persistence.xml.XCLModelPersistenceHandler;
import de.d3web.persistence.xml.mminfo.MMInfoPersistenceHandler;

public class D3webPersistence {
	private final static String CASE_REPOSITORY_HANDLER_DEFAULT_STORAGE_LOCATION = "cases/"
		+ "train.xml";
	
	private static D3webPersistence instance = new D3webPersistence();

	private PersistenceManager persistenceManager = PersistenceManager
			.getInstance();

	private D3webPersistence() {
		super();
		persistenceManager.addCaseRepositoryHandler(getCaseRepositoryHandler());
		persistenceManager.addPersistenceHandler(new XCLModelPersistenceHandler());
		persistenceManager.addPersistenceHandler(new MMInfoPersistenceHandler());
		DelegatePersistenceHandlerInitializer.initialize(persistenceManager);
	}

	public static D3webPersistence getInstance() {
		return instance;
	}

	public PersistenceManager getPersistenceManager() {
		return persistenceManager;
	}

	public CaseRepositoryHandler getCaseRepositoryHandler() {
		CaseRepositoryHandler crHandler = new de.d3web.caserepository.sax.CaseRepositoryHandler();

		de.d3web.caserepository.sax.CaseRepositoryHandler myHandler = (de.d3web.caserepository.sax.CaseRepositoryHandler) crHandler;

		myHandler.setId("train");
		myHandler.setStorageLocation(CASE_REPOSITORY_HANDLER_DEFAULT_STORAGE_LOCATION);
		myHandler.setWithProgressEvents(true);

		// shared

		myHandler.addTagReader(AppliedQSetsReader.getInstance());
		myHandler.addAdditionalWriter(new AppliedQSetsWriter());

		myHandler.addTagReader(ConfigReader.getInstance());
		myHandler.addAdditionalWriter(new ConfigWriter());

		// train

		myHandler.addTagReader(AdditionalTrainDataSAXReader.getInstance());
		myHandler.addAdditionalWriter(new AdditionalTrainDataWriter());

		myHandler.addTagReader(ContentsSAXReader.getInstance());
		myHandler.addAdditionalWriter(new ContentsWriter());

		myHandler.addTagReader(ExaminationBlocksSAXReader.getInstance());
		myHandler.addAdditionalWriter(new ExaminationBlocksWriter());

		myHandler.addTagReader(MultimediaSAXReader.getInstance());
		myHandler.addAdditionalWriter(new MultimediaWriter());

		myHandler
				.addTagReader(MultimediaSimpleQuestionsSAXReader.getInstance());
		myHandler.addAdditionalWriter(new MultimediaSimpleQuestionsWriter());

		myHandler.addTagReader(TemplateSessionReader.getInstance());
		myHandler.addAdditionalWriter(new TemplateSessionWriter());

		// fus

		myHandler.addTagReader(FUSConfigurationReader.getInstance());
		myHandler.addAdditionalWriter(new FUSConfigurationWriter());

		myHandler.addTagReader(SimpleTextFUSsReader.getInstance());
		myHandler.addAdditionalWriter(new SimpleTextFUSsWriter());

		return crHandler;
	}
}
