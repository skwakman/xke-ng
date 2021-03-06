package com.xebia.xkeng.assembly

import com.xebia.xkeng.dao._
import net.liftweb.util.Props
import net.liftweb.mongodb.{ DefaultMongoIdentifier, MongoDB }
import com.mongodb.{ Mongo, MongoOptions, ServerAddress }
import org.joda.time.DateTime
import org.bson.types.ObjectId
import net.liftweb.json._
import net.liftweb.common._
import net.liftweb.json.JsonDSL._
import com.xebia.xkeng.model.{ Session, Location, Conference, Author, Facility, AuthorDoc, Labels, SlotInfo, Comment }
import net.liftweb.mongodb.{ MongoDocument, ObjectIdSerializer, MongoDocumentMeta }
import com.mongodb.BasicDBObject
import com.xebia.xkeng.rest.XKENGPublicAPI
import com.xebia.xkeng.rest.XKENGSecuredAPI
import com.xebia.xkeng.rest.RestHandlerComponent
import com.xebia.xkeng.rest.ExceptionHandler

object Assembly extends Logger {

  trait RepositoryComponentImpl extends RepositoryComponent {
    val conferenceRepository = new ConferenceRepositoryImpl
    val sessionRepository = new SessionRepositoryImpl
    val facilityRepository = new FacilityRepositoryImpl
    val authorRepository = new AuthorRepositoryImpl
    val labelRepository = new LabelRepositoryImpl

    private val crowdSysUser = Props.get("crowd.sysuser").get
    private val crowdSysUserPwd = Props.get("crowd.sysuser.pwd").get
    private val crowdBase = Props.get("crowd.base.url").get
    private val crowdConnectionCheck = Props.get("crowd.conn.check").map(_.trim.toBoolean).getOrElse(false)
    val authenticationRepository = AuthenticationRepository(crowdBase, crowdSysUser, crowdSysUserPwd, crowdConnectionCheck)

  }

  trait DummySecurityRepositoryComponentImpl extends RepositoryComponentImpl {
    override val authenticationRepository = new DummyAuthenticationRepositoryImpl()
  }

  trait RestHandlerComponentImpl extends RestHandlerComponent with RepositoryComponentImpl
  object XKENGSecuredAPIAssembly extends XKENGSecuredAPI with RestHandlerComponentImpl 
  class XKENGPublicAPIAssembly extends XKENGPublicAPI with RestHandlerComponentImpl 
  object ExceptionHandlerAssembly extends ExceptionHandler

  def init() = {
    initMongoDB()
  }

  private def initMongoDB() = {
    val srvr = new ServerAddress(Props.get("mongo.host").getOrElse("127.0.0.1"), Props.get("mongo.port").map(_.toInt).getOrElse(27017))
    val mo = new MongoOptions
    mo.socketTimeout = Props.get("mongo.socket.timeout").map(_.toInt).getOrElse(30000)
    info("MongoDB connect properties= " + srvr)
    info("MongoDB properties= " + mo)
    MongoDB.defineDb(DefaultMongoIdentifier, new Mongo(srvr, mo), Props.get("mongo.db.name").getOrElse("xkeng"))
    ensureIndexes()

    //Unittests with maven fail when indexes are ensured in ConferenceMetaData class. Here it works...
    def ensureIndexes() {
      Conference.ensureIndex(("begin" -> 1))
      Conference.ensureIndex(("sessions.id" -> -1))
      Conference.ensureIndex(("locations.id" -> -1))
    }
  }

  def purgeAndPushTestdata(forcePurge: Boolean = false) = {
    if (Conference.count == 0 || Facility.count == 0 || AuthorDoc.count == 0 || Labels.count == 0 || forcePurge) {
      Conference.drop
      Facility.drop
      AuthorDoc.drop
      Labels.drop
      val today = new DateTime().hourOfDay.setCopy(16).minuteOfHour.setCopy(0)
      val prevWeek = today.minusWeeks(1)
      val nextWeek = today.plusWeeks(1)
      val dates = today :: prevWeek :: nextWeek :: Nil
      val locations = createTestFacility
      val authors = createTestAuthors
      dates.map(createTestConferences(_, locations, authors))
    }
  }

  private def createTestFacility: List[Location] = {
    import XKENGSecuredAPIAssembly.facilityRepository._
    val l1 = Location("Maup", 20)
    val l2 = Location("Laap", 30)
    val l3 = Location("Library", 10)
    val locations = List(l1, l2, l3)
    locations.map(addLocation(_))
    locations
  }
  private def createTestAuthors: List[Author] = {
    import XKENGSecuredAPIAssembly.authorRepository._
    val a1 = Author("peteru", "upeter@xebia.com", "Urs Peter")
    val a2 = Author("amooy", "amooy@xebia.com", "Age Mooy")
    val authors = List(a1, a2)
    authors.map(addAuthor(_))
    authors
  }

  private def createTestConferences(startDate: DateTime, locations: List[Location], authors: List[Author]) = {
    val a1 = authors(0)
    val a2 = authors(1)
    val slot1Start = startDate
    val slot1End = startDate.plusMinutes(60)
    val slot2Start = slot1End
    val slot2End = slot2Start.plusMinutes(60)
    val schedule = SlotInfo(slot1Start, slot1End) :: SlotInfo(slot2Start, slot2End) :: Nil
    val s1 = Session(slot1Start, slot1End, locations(0), "Mongo rocks", "Mongo is a paperless document database", "STRATEGIC", "10 people", List(a1), Nil, List(Comment("Awesome session", "upeter"), Comment("Amazing technology", "amooy")), Set("Database", "Mongo", "Javascript"))
    val s2 = Session(slot1Start, slot1End, locations(1), "Scala rocks even more", "Scala is a codeless programming language", "STRATEGIC", "10 people", List(a2), Nil, Nil, Set("Scala", "Functions", "DSL"))
    val s3 = Session(slot1Start, slot2End, locations(2), "Zero Maintenance Middleware - A Continuous Delivery Concept", "Richard Pot and Erwin Embsen are developing / implementing Zero Maintenance Middleware at KVK. ZMM deals with the lifecycle of applications, operating systems and middleware, and is based on VMWare, Puppet and Deployit. ", "STRATEGIC", "15 people", List(a1, a2), Nil, Nil, Set("Scala", "Functional Programming", "Beauty"))
    val s4 = Session(slot2Start, slot2End, locations(0), "Deployment with puppet", "Even animals start to like deploying", "STRATEGIC", "20 people", List(a1, a2), Nil, Nil, Set("Middleware", "Deoployment", "Quick"))
    val c = Conference("Test-XKE", startDate, startDate.plusHours(4), Nil, locations, schedule)
    c.save
    val startDate2 = startDate.plusDays(14)
    val c2 = Conference("NextTest-XKE", startDate2, startDate2.plusHours(4), Nil, locations, schedule)
    c2.save
    List(s1, s2, s3, s4).foreach(c.saveOrUpdate(_))
  }
}