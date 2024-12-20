sig DateTime {}
// Abstract User Signature
abstract sig User {}
// Student Extends User
sig Student extends User {
}
// Company Extends User
sig Company extends User {
    internships: set Internship 
}
// Internship Entity
sig Internship {}

abstract sig Communication { 
}
sig Complaint extends Communication { 
	var status: one Int
}
fact statusRange {
	all c : Complaint | {
		always (c.status >= -1 and c.status <= 2)
}
}
fact complaintEvolution1 {
	all c : Complaint | {
		always (c.status = -1 implies (c.status' = -1 or c.status' = 0))
	}
}
fact complaintEvolution2 {
	all c : Complaint | {
		always (c.status = 0 implies (c.status' = 0 or c.status' = 1))
}
}
fact complaintEvolution3 {
	all c : Complaint | {
		always (c.status = 1 implies (c.status' = 1 or c.status' = 2))
}
}
fact complaintEvolution4 {
	all c : Complaint | {
		always (c.status = 2 implies (c.status' = 2))
}
}
fact complaintLink2Internship {
    all c: Complaint | {
        always ((some shi: StudentHasInternship | c in shi.communication) implies (c.status = 0 or c.status = 1 or c.status = 2))
	  and always ((c.status = 0 or c.status = 1 or c.status = 2) implies (some shi: StudentHasInternship | c in shi.communication))
}
}
fact complaintEvolveDuringInternship {
	all c: Complaint | {
		always (c.status != c.status') implies (some shi: StudentHasInternship | c in shi.communication)
	}

}

sig Feedback extends Communication {
} 

sig Questionnaire {}

sig InterviewSchedule {}

sig Survey {}

enum State { requestAccepted, selectionPassed, selectionNotPassed, internshipEnded}

sig StudentHasInternship {
    var state: State,                   // needed to model the various states the internship might be in
    var startDate: lone DateTime ,             // Start date defined after selection
    var endDate: lone DateTime,               // End date defined after selection (can change)
    student: one Student,             // Linked student
    internship: one Internship,       // Linked internship
    var interviewSchedule: lone InterviewSchedule, // Interview schedule (optional but lone)
    var questionnaire: lone Questionnaire, // Questionnaire (optional but lone)
    var communication: set Communication,  // Abstract communication: Complaints or Feedbacks
    var survey: set Survey                 // Surveys, only during internship
}

fact diffStartEndDate {
	all shi : StudentHasInternship | {
		always(
			(shi.state != requestAccepted and shi.state != selectionNotPassed) implies (shi.endDate != shi.startDate))
	}
}

fact "init" {
	all shi: StudentHasInternship | {
		shi.state = requestAccepted
	}
	all c : Complaint | {
		c.status = -1
	}
}
fact sinkInternshipEnd {
	all shi : StudentHasInternship | {
		always(
			shi.state = internshipEnded implies always shi.state' = internshipEnded 
		)
	}
}
fact sinkSelectionNotPassed {
	all shi : StudentHasInternship | {
		always(
			shi.state = selectionNotPassed implies always shi.state' = selectionNotPassed
		)
	}
}
fact requestAccepted2Selection {
	all shi : StudentHasInternship | {
		always(
			shi.state = requestAccepted implies (shi.state' = requestAccepted or shi.state' = selectionPassed or shi.state' = selectionNotPassed) 
		)
	}
}
fact SelectionPassed2End {
	all shi : StudentHasInternship | {
		always ( 
			shi.state = selectionPassed implies (shi.state' = internshipEnded or shi.state' = selectionPassed)
		)
	}
}
fact internshipWillEnd {
	all shi : StudentHasInternship | {
		always ( 
			shi.state = selectionPassed implies eventually shi.state' = internshipEnded
		)
	}
}
fact SelectionProcessWillEnd {
	all shi : StudentHasInternship | {
		always ( 
			shi.state = requestAccepted implies eventually (shi.state' = selectionPassed or shi.state' = selectionNotPassed)
		)
	}
}
fact constraint1 {
	all shi: StudentHasInternship | {
		always(
			shi.state = requestAccepted implies (shi.startDate = none and shi.endDate = none and shi.survey = none and shi.communication = none))
}
}
fact constraint2 {
	all shi: StudentHasInternship  | {
		always(
			shi.state = selectionPassed implies (shi.startDate != none and shi.endDate != none and shi.interviewSchedule' = shi.interviewSchedule and shi.questionnaire' = shi.questionnaire and shi.startDate' = shi.startDate and shi.communication & Feedback = none and complaintsDontDisappear[shi]))
}
}
fact constraint3 {
	all shi: StudentHasInternship | {
		always(
			shi.state = selectionNotPassed implies (shi.startDate = none and shi.endDate = none and shi.survey = none and shi.communication = none and shi.interviewSchedule' = shi.interviewSchedule and shi.questionnaire' = shi.questionnaire))
}
}
fact constraint4 {
	all shi: StudentHasInternship | {
		always(
			shi.state = internshipEnded implies (shi.startDate != none and shi.endDate != none and shi.interviewSchedule' = shi.interviewSchedule and shi.questionnaire' = shi.questionnaire and shi.startDate' = shi.startDate))
}
}
fact transitionConstraint1 {
	all shi: StudentHasInternship | {
		always(
			(shi.state' = selectionPassed and shi.state = requestAccepted) implies (shi.questionnaire' = shi.questionnaire and shi.interviewSchedule' = shi.interviewSchedule and shi.startDate' != shi.startDate))
}
}
fact transitionConstraint2 {
	all shi: StudentHasInternship | {
		always(
			(shi.state' = internshipEnded and shi.state = selectionPassed) implies (shi.questionnaire' = shi.questionnaire and shi.interviewSchedule' = shi.interviewSchedule and shi.startDate' = shi.startDate and shi.survey' = shi.survey and allComplaintsResolved[shi] and shi.communication' & Complaint = shi.communication & Complaint))
}
}
pred allComplaintsResolved[shi : StudentHasInternship]{
	all c : Complaint | c in shi.communication implies c.status = 2 
}
pred complaintsDontDisappear[shi : StudentHasInternship]{
	all c : Complaint | c in shi.communication implies c in shi.communication'
}
pred interviewOccured[shi : StudentHasInternship]{
	all shi: StudentHasInternship | {
		eventually shi.interviewSchedule != none
}
}
pred complaintOccured[shi : StudentHasInternship]{
	all shi: StudentHasInternship | {
		eventually shi.communication & Complaint != none
}
}
pred feedbackOccured[shi : StudentHasInternship]{
	all shi: StudentHasInternship | {
		eventually shi.communication & Feedback!= none
}
}

pred show[shi: StudentHasInternship] {
#StudentHasInternship = 1
#Company = 1
#Student = 1
#Internship = 1
#Feedback = 1
#Complaint = 1
eventually shi.state = selectionPassed
eventually (shi.state' = selectionPassed and shi.state = selectionPassed)
eventually interviewOccured[shi]
eventually complaintOccured[shi]
eventually feedbackOccured[shi]
}
pred show1[shi: StudentHasInternship] {
#StudentHasInternship = 1
#Company = 1
#Student = 1
#Internship = 1
eventually shi.state = selectionNotPassed
eventually interviewOccured[shi]

}
run show






















































































