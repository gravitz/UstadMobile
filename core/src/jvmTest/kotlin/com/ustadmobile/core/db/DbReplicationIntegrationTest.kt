package com.ustadmobile.core.db

import com.ustadmobile.core.account.*
import com.ustadmobile.core.db.ext.addSyncCallback
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UstadMobileConstants
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.DiTag
import com.ustadmobile.core.util.UstadTestRule
import com.ustadmobile.core.util.ext.enrolPersonIntoClazzAtLocalTimezone
import com.ustadmobile.core.util.ext.getOrGenerateNodeIdAndAuth
import com.ustadmobile.core.util.ext.grantScopedPermission
import com.ustadmobile.core.util.ext.insertPersonAndGroup
import com.ustadmobile.core.util.test.waitUntil
import com.ustadmobile.core.util.test.waitUntilAsync
import com.ustadmobile.door.DatabaseBuilder
import com.ustadmobile.door.DoorDatabaseRepository
import com.ustadmobile.door.RepositoryConfig
import com.ustadmobile.door.entities.NodeIdAndAuth
import com.ustadmobile.door.ext.*
import com.ustadmobile.door.replication.doorReplicationRoute
import com.ustadmobile.door.util.NodeIdAuthCache
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.rest.ext.initAdminUser
import com.ustadmobile.lib.rest.ext.ktorInitRepo
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Assert
import org.kodein.di.*
import org.xmlpull.v1.XmlPullParserFactory
import org.junit.rules.TemporaryFolder
import org.kodein.di.ktor.DIFeature
import java.io.File
import kotlin.random.Random


class DbReplicationIntegrationTest {

    private lateinit var remoteServer: ApplicationEngine

    private lateinit var remoteDi: DI

    private lateinit var remoteDb: UmAppDatabase

    private val localDb: UmAppDatabase
        get() = localDi.on(Endpoint(TEST_SERVER_HOST)).direct.instance(tag = DoorTag.TAG_DB)

    private val localDbRepo: UmAppDatabase
        get() = localDi.on(Endpoint(TEST_SERVER_HOST)).direct.instance(tag = DoorTag.TAG_REPO)

    private lateinit var remoteVirtualHostScope: EndpointScope

    private lateinit var httpClient: HttpClient

    private lateinit var okHttpClient: OkHttpClient

    private lateinit var jsonSerializer: Json

    private lateinit var localDi: DI



    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Rule
    @JvmField
    var ustadTestRule = UstadTestRule(
        repoReplicationSubscriptionEnabled = true,
        repSubscriptionInitListener = RepSubscriptionInitListener()
    )

    @Before
    fun setup() {
        Napier.takeLogarithm()
        Napier.base(DebugAntilog())

        okHttpClient = OkHttpClient.Builder().build()

        httpClient = HttpClient(OkHttp) {
            install(JsonFeature)
            engine {
                preconfigured = okHttpClient
            }
        }

        remoteVirtualHostScope = EndpointScope()
        val nodeIdAndAuth = NodeIdAndAuth(Random.nextLong(0, Long.MAX_VALUE), "secret")
        remoteDb = DatabaseBuilder.databaseBuilder(Any(), UmAppDatabase::class, "UmAppDatabase")
            .addSyncCallback(nodeIdAndAuth)
            .addCallback(ContentJobItemTriggersCallback())
            .build().also { db ->
                db.clearAllTablesAndResetNodeId(nodeIdAndAuth.nodeId)
                db.addIncomingReplicationListener(RepIncomingListener(db))
            }

        remoteDi = DI {
            bind<UstadMobileSystemImpl>() with singleton {
                UstadMobileSystemImpl(instance(tag  = DiTag.XPP_FACTORY_NSAWARE),
                    tempFolder.newFolder())
            }

            bind<XmlPullParserFactory>(tag  = DiTag.XPP_FACTORY_NSAWARE) with singleton {
                XmlPullParserFactory.newInstance().also {
                    it.isNamespaceAware = true
                }
            }

            bind<UmAppDatabase>(tag = DoorTag.TAG_DB) with scoped(remoteVirtualHostScope).singleton {
                remoteDb
            }

            bind<HttpClient>() with singleton {
                httpClient
            }

            bind<OkHttpClient>() with singleton {
                okHttpClient
            }

            bind<UmAppDatabase>(tag = DoorTag.TAG_REPO) with scoped(remoteVirtualHostScope).singleton {
                val db = instance<UmAppDatabase>(tag = DoorTag.TAG_DB)
                val doorNode = instance<NodeIdAndAuth>()
                val repo: UmAppDatabase = db.asRepository(RepositoryConfig.repositoryConfig(
                    Any(), "http://localhost/",
                    doorNode.nodeId, doorNode.auth, instance(), instance()
                ) {
                    useReplicationSubscription = false
                    attachmentsDir = tempFolder.newFolder().absolutePath
                })



                repo
            }

            bind<NodeIdAndAuth>() with scoped(remoteVirtualHostScope).singleton {
                nodeIdAndAuth
            }

            bind<NodeIdAuthCache>() with scoped(remoteVirtualHostScope).singleton {
                instance<UmAppDatabase>(tag = DoorTag.TAG_DB).nodeIdAuthCache
            }

            bind<File>(tag = DiTag.TAG_CONTEXT_DATA_ROOT) with scoped(remoteVirtualHostScope).singleton {
                tempFolder.newFolder("contextroot")
            }

            bind<Pbkdf2Params>() with singleton {
                val systemImpl: UstadMobileSystemImpl = instance()
                val numIterations = systemImpl.getAppConfigInt(
                    AppConfig.KEY_PBKDF2_ITERATIONS,
                    UstadMobileConstants.PBKDF2_ITERATIONS, context)
                val keyLength = systemImpl.getAppConfigInt(
                    AppConfig.KEY_PBKDF2_KEYLENGTH,
                    UstadMobileConstants.PBKDF2_KEYLENGTH, context)

                Pbkdf2Params(numIterations, keyLength)
            }

            bind<AuthManager>() with scoped(EndpointScope.Default).singleton {
                AuthManager(context, di)
            }

            registerContextTranslator { _: ApplicationCall -> Endpoint("localhost") }
        }

        val remoteRepo: UmAppDatabase = remoteDi.direct.on(Endpoint("localhost"))
            .instance(tag = DoorTag.TAG_REPO)
        remoteRepo.preload()
        remoteRepo.ktorInitRepo()
        runBlocking {
            remoteRepo.initAdminUser(Endpoint("localhost"), remoteDi)
        }


        localDi = DI {
            import(ustadTestRule.diModule)
        }

        jsonSerializer = Json {
            encodeDefaults = true
        }



        remoteServer = embeddedServer(Netty, 8089, configure = {
            requestReadTimeoutSeconds = 600
            responseWriteTimeoutSeconds = 600
        }) {
            install(DIFeature){
                extend(remoteDi)
            }

            routing {
                route("UmAppDatabase") {
                    UmAppDatabase_KtorRoute()
                }
            }
        }
        remoteServer.start()
    }

    @After
    fun stop() {
        remoteServer.stop(1000, 1000)
    }

    @Test
    fun givenUserSessionCreated_whenContentEntryAdded_thenShouldReplicate() {
        //create a local session
        val contentEntry = ContentEntry().apply {
            title = "Hello World"
            contentEntryUid = remoteDb.contentEntryDao.insert(this)
        }

        val accountManager: UstadAccountManager by localDi.instance()
        val adminPerson = remoteDb.personDao.findByUsername("admin") !!

        //put the person who just "logged in" in the local database
        localDb.personDao.insert(adminPerson)

        runBlocking {
            accountManager.addSession(adminPerson, TEST_SERVER_HOST, "secret")
        }

        //wait for contententry to land...
        runBlocking {
            localDb.waitUntil(10001, listOf("ContentEntry")) {
                localDb.contentEntryDao.findByUid(contentEntry.contentEntryUid) != null
            }
        }

        //now create a second one
        val contentEntry2 = ContentEntry().apply {
            title = "Hello World 2"
            contentEntryUid = remoteDb.contentEntryDao.insert(this)
        }

        runBlocking {
            localDb.waitUntil(10002, listOf("ContentEntry")) {
                localDb.contentEntryDao.findByUid(contentEntry2.contentEntryUid) != null
            }
        }


        Assert.assertNotNull(localDb.contentEntryDao.findByUid(contentEntry.contentEntryUid))
        Assert.assertNotNull(localDb.contentEntryDao.findByUid(contentEntry2.contentEntryUid))
    }

    @Test
    fun givenUserSessionCreated_whenConnectedAndClazzCreated_thenScopedGrantShouldReplicateAndClazzShouldReplicate() {
        val accountManager: UstadAccountManager by localDi.instance()
        val adminPerson = remoteDb.personDao.findByUsername("admin") !!

        //put the person who just "logged in" in the local database
        localDb.personDao.insert(adminPerson)

        runBlocking {
            accountManager.addSession(adminPerson, TEST_SERVER_HOST, "secret")
        }

        //wait for contententry to land...
        runBlocking {
            localDb.waitUntilAsync(10003, listOf("ScopedGrant")) {
                localDb.scopedGrantDao.findByTableIdAndEntityUid(ScopedGrant.ALL_TABLES,
                    ScopedGrant.ALL_ENTITIES).firstOrNull { it.scopedGrant?.sgGroupUid == adminPerson.personGroupUid } != null
            }
        }

        val replicatedScopedGrant = runBlocking {
            localDb.scopedGrantDao.findByTableIdAndEntityUid(ScopedGrant.ALL_TABLES,
                ScopedGrant.ALL_ENTITIES).firstOrNull { it.scopedGrant?.sgGroupUid == adminPerson.personGroupUid }
        }

        Assert.assertNotNull(replicatedScopedGrant)

        val clazz = Clazz().apply {
            this.clazzName = "Test class"
            clazzUid = remoteDb.clazzDao.insert(this)
        }

        runBlocking {
            localDb.waitUntil(10004, listOf("Clazz")) {
                localDb.clazzDao.findByUid(clazz.clazzUid) != null
            }
        }

        Assert.assertNotNull(localDb.clazzDao.findByUid(clazz.clazzUid))

    }

    @Test
    fun givenClazzCreatedOnServer_whenUserWithScopedPermissionConnected_thenShouldReplicateClazzWithPermissionAndNotOthers() {
        val localAccountManager: UstadAccountManager by localDi.instance()

        //create the new user
        val teacherPerson = runBlocking {
            remoteDb.insertPersonAndGroup(Person().apply {
                firstNames = "Teacher"
                lastName = "Teacher"
                username = "teacher"
            })
        }

        val newClazz = Clazz().apply {
            clazzName = "Test clazz"
            clazzUid = remoteDb.clazzDao.insert(this)
        }

        val anotherClazz = Clazz().apply {
            clazzName = "Other clazz"
            clazzUid = remoteDb.clazzDao.insert(this)
        }

        runBlocking {
            remoteDb.grantScopedPermission(teacherPerson,
                Role.ROLE_CLAZZ_TEACHER_PERMISSIONS_DEFAULT, Clazz.TABLE_ID, newClazz.clazzUid)
        }

        localDb.personDao.insert(teacherPerson)
        runBlocking {
            localAccountManager.addSession(teacherPerson, TEST_SERVER_HOST, "secret")
        }

        runBlocking {
            localDb.waitUntil(10006, listOf("Clazz")) {
                localDb.clazzDao.findByUid(newClazz.clazzUid) != null
            }
        }

        Assert.assertNotNull(localDb.clazzDao.findByUid(newClazz.clazzUid))

        //make sure that the other class does not come
        Thread.sleep(1000)
        Assert.assertNull(localDb.clazzDao.findByUid(anotherClazz.clazzUid))
    }

    @Test
    fun givenUserLoggedInWithPermissionOnClazz_whenNewPersonEnroledInClazz_thenShouldReplicateRelatedEntities() {
        val localAccountManager: UstadAccountManager by localDi.instance()

        //create the new user
        val teacherPerson = runBlocking {
            remoteDb.insertPersonAndGroup(Person().apply {
                firstNames = "Teacher"
                lastName = "Teacher"
                username = "teacher"
            })
        }

        val studentPerson = runBlocking {
            remoteDb.insertPersonAndGroup(Person().apply {
                firstNames = "Student"
                lastName = "student"
                username = "student"
            })
        }

        val newClazz = Clazz().apply {
            clazzName = "Test clazz"
            clazzUid = remoteDb.clazzDao.insert(this)
        }

        runBlocking {
            remoteDb.grantScopedPermission(teacherPerson,
                Role.ROLE_CLAZZ_TEACHER_PERMISSIONS_DEFAULT, Clazz.TABLE_ID, newClazz.clazzUid)
        }

        localDb.personDao.insert(teacherPerson)
        runBlocking {
            localAccountManager.addSession(teacherPerson, TEST_SERVER_HOST, "secret")
        }

        runBlocking {
            localDb.waitUntil(10007, listOf("Clazz")) {
                localDb.clazzDao.findByUid(newClazz.clazzUid) != null
            }
        }

        runBlocking {
            remoteDb.enrolPersonIntoClazzAtLocalTimezone(studentPerson, newClazz.clazzUid,
                ClazzEnrolment.ROLE_STUDENT)
            val localDbNodeId = (localDbRepo as DoorDatabaseRepository).config.nodeId
            remoteDb.replicationNotificationDispatcher.onNewDoorNode(localDbNodeId, "")

            localDb.waitUntil(10008, listOf("Person")) {
                localDb.personDao.findByUsername(studentPerson.username) != null
            }
        }


    }


    companion object {

        const val TEST_SERVER_HOST = "http://localhost:8089/"

        const val TEST_SERVER_ENDPOINT = "${TEST_SERVER_HOST}UmAppDatabase"

    }

}