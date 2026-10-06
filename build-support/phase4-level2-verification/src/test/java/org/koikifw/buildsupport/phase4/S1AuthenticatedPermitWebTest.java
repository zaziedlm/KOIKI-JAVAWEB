package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.koikifw.buildsupport.phase4.s1fixture.S1CStore.*;

import jakarta.servlet.http.Cookie;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.s1fixture.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** C1/C2: Cookie/CSRF from actual login, real local Provider, JDBC Session, no mock principal. */
@Timeout(600)
class S1AuthenticatedPermitWebTest {
    private static final S1CDatabase DB=new S1CDatabase();
    private static long started;
    @BeforeAll static void start(){started=System.nanoTime();DB.start();}
    @AfterAll static void stop(){try{assertTrue((System.nanoTime()-started)/1_000_000_000<600);}finally{DB.close();}}
    @BeforeEach void seed(){DB.seed();}
    @AfterEach void after(){System.out.println("S1-C Web after permit="+DB.count("s1c.permit")+" consumption="+DB.count("s1c.consumption")+" session="+DB.count("koiki_session")+" audit="+DB.count("koiki_audit_event"));}
    public record Browser(Cookie cookie,String csrf) {}
    public static Cookie cookie(MvcResult result,Cookie previous) {
        for(String header:result.getResponse().getHeaders("Set-Cookie")) {
            var match=Pattern.compile("^SESSION=([^;]+)").matcher(header);
            if(match.find())return new Cookie("SESSION",match.group(1));
        }
        if(previous==null)throw new IllegalStateException("No real Session cookie");return previous;
    }
    public static Browser login(MockMvc mvc,String name) throws Exception {
        var page=mvc.perform(get("/login").secure(true)).andReturn();
        var matcher=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page.getResponse().getContentAsString());
        assertTrue(matcher.find(),"CSRF must come from actual login page");
        Cookie initial=cookie(page,null);
        var result=mvc.perform(post("/login").secure(true).cookie(initial).param("_csrf",matcher.group(1))
                .param("email",name+"@fixture.invalid").param("password",S1CDatabase.PASSWORD)).andReturn();
        assertEquals(302,result.getResponse().getStatus());assertEquals("/s1-test/home",result.getResponse().getRedirectedUrl());
        Cookie authenticated=cookie(result,initial);
        assertNotEquals(initial.getValue(),authenticated.getValue(),"Session fixation protection");
        var home=mvc.perform(get("/s1-test/home").secure(true).cookie(authenticated)).andReturn();
        assertEquals(200,home.getResponse().getStatus());assertTrue(home.getResponse().getContentAsString().startsWith("CSRF:"));
        return new Browser(cookie(home,authenticated),home.getResponse().getContentAsString().substring(5));
    }
    public static MvcResult request(MockMvc mvc,Browser browser,String operation,boolean csrf,String... params) throws Exception {
        MockHttpServletRequestBuilder request=operation.equals("read")?get("/s1-test/read"):post("/s1-test/"+operation);
        request.secure(true).cookie(browser.cookie);
        if(csrf)request.param("_csrf",browser.csrf);
        for(int i=0;i<params.length;i+=2)request.param(params[i],params[i+1]);
        var result=mvc.perform(request).andReturn();
        assertEquals("nosniff",result.getResponse().getHeader("X-Content-Type-Options"));
        assertEquals("DENY",result.getResponse().getHeader("X-Frame-Options"));return result;
    }
    private static void web(Checked action) {
        S1CWebFixture.run(DB.url(),false,context->{try{action.run(S1CWebFixture.mvc(context));}catch(Exception failure){throw new IllegalStateException(failure);}});
    }
    private interface Checked{void run(MockMvc mvc)throws Exception;}
    @Test void c1_01_authenticatedActorAndSessionPersist() {
        DB.sql("DELETE FROM s1c.permit");
        web(mvc->{var browser=login(mvc,"issuer");assertTrue(DB.count("koiki_session")>=1);
            assertEquals(user("issuer").toString(),DB.observe("SELECT principal_name FROM koiki_session WHERE principal_name IS NOT NULL LIMIT 1"));
            assertEquals(200,request(mvc,browser,"issue",true).getResponse().getStatus());
            assertEquals(user("issuer").toString(),DB.observe("SELECT actor_id FROM s1c.permit"));
            assertEquals(user("issuer").toString(),DB.observe("SELECT actor_id FROM koiki_audit_event WHERE audit_type='BUSINESS'"));
            assertEquals(1,DB.count("s1c.permit"));assertEquals(0,DB.count("s1c.consumption"));
            assertTrue(mvc.perform(get("/unrelated").secure(true).cookie(browser.cookie)).andReturn().getResponse().getStatus()>=400);
        });
    }
    @ParameterizedTest @ValueSource(strings={"wrongPassword","disabled"})
    void c1_02_realAuthenticationFailure(String failure) {
        String before=DB.snapshot();
        web(mvc->{var page=mvc.perform(get("/login").secure(true)).andReturn();
            var token=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page.getResponse().getContentAsString());assertTrue(token.find());
            var result=mvc.perform(post("/login").secure(true).cookie(cookie(page,null)).param("_csrf",token.group(1))
                    .param("email",failure.equals("disabled")?"disabled@fixture.invalid":"issuer@fixture.invalid")
                    .param("password",failure.equals("wrongPassword")?"incorrect-test-password":S1CDatabase.PASSWORD)).andReturn();
            assertEquals(302,result.getResponse().getStatus());assertEquals("/login?error",result.getResponse().getRedirectedUrl());
            assertEquals(before,DB.snapshot());
        });
    }
    @ParameterizedTest @ValueSource(strings={"issue","read","close"})
    void c2_01_unauthenticatedOperation(String operation) {
        String before=DB.snapshot();
        web(mvc->{var page=mvc.perform(get("/login").secure(true)).andReturn();
            var token=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page.getResponse().getContentAsString());assertTrue(token.find());
            var result=request(mvc,new Browser(cookie(page,null),token.group(1)),operation,true);
            assertEquals(302,result.getResponse().getStatus());assertTrue(result.getResponse().getRedirectedUrl().endsWith("/login"));
            assertEquals(before,DB.snapshot());
        });
    }
    @ParameterizedTest @ValueSource(strings={"issue","read","close"})
    void c2_02_separateAbilities(String operation) {
        if(operation.equals("issue"))DB.sql("DELETE FROM s1c.permit");String before=DB.snapshot();
        web(mvc->{var browser=login(mvc,operation.equals("read")?"limited":"reader");
            assertEquals(403,request(mvc,browser,operation,true).getResponse().getStatus());assertEquals(before,DB.snapshot());
            var allowed=login(mvc,operation.equals("close")?"closer":operation.equals("read")?"reader":"issuer");
            assertEquals(200,request(mvc,allowed,operation,true).getResponse().getStatus());
        });
    }
    @ParameterizedTest @ValueSource(strings={"login","issue","close"})
    void c2_03_missingCsrf(String operation) {
        String before=DB.snapshot();
        web(mvc->{if(operation.equals("login"))assertEquals(403,mvc.perform(post("/login").secure(true).param("email","issuer@fixture.invalid").param("password",S1CDatabase.PASSWORD)).andReturn().getResponse().getStatus());
            else assertEquals(403,request(mvc,login(mvc,operation.equals("close")?"closer":"issuer"),operation,false).getResponse().getStatus());
            assertEquals(before,DB.snapshot());
        });
    }
    @ParameterizedTest @ValueSource(strings={"issue","read","close"})
    void c2_04_scopeOutside(String operation) {
        String before=DB.snapshot();
        web(mvc->{var result=request(mvc,login(mvc,"outsider"),operation,true);
            assertEquals(403,result.getResponse().getStatus());assertEquals("S1_TEST_DENIED",result.getResponse().getContentAsString());assertEquals(before,DB.snapshot());
        });
    }
    @ParameterizedTest @ValueSource(strings={"issue","close"})
    void c2_05_actorInputTampering(String operation) {
        String before=DB.snapshot();
        web(mvc->{assertEquals(400,request(mvc,login(mvc,operation.equals("close")?"closer":"issuer"),operation,true,"actorId",user("outsider").toString()).getResponse().getStatus());assertEquals(before,DB.snapshot());});
    }
}
