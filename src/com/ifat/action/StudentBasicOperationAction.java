package com.ifat.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ifat.config.GetHttpSessionConfigurator;
import com.ifat.model.Question;
import com.ifat.model.Student;
import com.ifat.service.StudentService;
import com.opensymphony.xwork2.ModelDriven;
import com.sun.mail.util.QEncoderStream;

@ServerEndpoint(value = "/ws/websocket", configurator = GetHttpSessionConfigurator.class)
public class StudentBasicOperationAction extends SuperAction implements
		ModelDriven<Student> {

	private static final long serialVersionUID = 1L;
	private Student student;
	private StudentService studentService;
	private HttpSession httpSession;
	/**
	 * 存储当前有效的session对象
	 */
	private static Queue<Session> sessionSet = new ConcurrentLinkedQueue<Session>();

	public StudentService getStudentService() {
		return studentService;
	}

	public void setStudentService(StudentService studentService) {
		this.studentService = studentService;
	}

	public static Queue<Session> getSessionSet() {
		return sessionSet;
	}

	/**
	 * 处理答题。
	 * 
	 * @return
	 */
	public String dealWithAnswerQuestion(String message) {
		@SuppressWarnings("unchecked")
		ArrayList<Question> questions = (ArrayList<Question>) httpSession
				.getAttribute("questionList");
		int tag = Integer.parseInt(message.substring(0, 1));
		Question question = questions.get(tag - 1);
		String answer = message.substring(1, message.length()).trim()
				.toLowerCase();
		int times = question.getTimes();
		String result = "";

		if (answer.equals(question.getAnswer().trim().toLowerCase())) {

			if (times == 0) {
				question.setScore(4);
			} else if (times == 1) {
				question.setScore(3);
			} else if (times == 2) {
				question.setScore(2);
			} else if (times == 3) {
				question.setScore(1);
			}

			result = message + " 第" + (question.getTimes() + 1) + "次答题正确，得"
					+ question.getScore() + "分";
		} else {
			question.setTimes(++times);
			result = message + " 答案错误,请继续答题";
		}
//		questions.remove(tag - 1);
		questions.set(tag - 1, question);
		httpSession.setAttribute("questionList", questions);
		return result;
	}

	/**
	 * 接收消息。
	 * 
	 * @param message
	 * @param currentSession
	 */
	@OnMessage
	public void onMessage(String message, Session currentSession) {
		try {
			final Set<Session> sessions = currentSession.getOpenSessions();

			for (Session s : sessions) {
				s.getBasicRemote().sendText(dealWithAnswerQuestion(message));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 打开连接。
	 * 
	 * @param currentSession
	 */
	@OnOpen
	public void onOpen(Session currentSession, EndpointConfig config) {
		httpSession = (HttpSession) config.getUserProperties().get(
				HttpSession.class.getName());

		if (sessionSet.contains(currentSession) == false) {
			sessionSet.add(currentSession);
		}
	}

	/**
	 * 关闭连接。
	 * 
	 * @param currentSession
	 */
	@OnClose
	public void onClose(Session currentSession) {
		if (sessionSet.contains(currentSession)) {
			sessionSet.remove(currentSession);
		}
	}

	/**
	 * 学生登录处理。
	 * 
	 * @return
	 */
	public String login() {
		if ("studentLoginSuccess".equals(studentService.dealWithLogin(student))) {
			student = (Student) studentService.getStudentDAO()
					.findByName(student.getName()).get(0);
			session.setAttribute("studentId", student.getId());
			session.setAttribute("studentName", student.getName());
			// TODO 显示试题.
			displayQuestionnaire();
			return "studentLoginSuccess";
		}

		request.setAttribute("info", studentService.dealWithLogin(student));

		return "studentLoginFailed";
	}

	/**
	 * 显示试卷
	 * 
	 * @return
	 */
	public String displayQuestionnaire() {
		student.setCid(session.getAttribute("studentId").toString());

		ArrayList<Question> questions = studentService
				.dealWithDisplayQuestionnaire(student);
		request.setAttribute("questionList", questions);
		session.setAttribute("questionList", questions);
		return "displayQuestionnaireSuccess";
	}

	@Override
	public Student getModel() {

		if (student == null) {
			student = new Student();
		}
		return student;
	}

}
