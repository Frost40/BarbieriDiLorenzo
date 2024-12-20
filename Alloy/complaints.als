sig Text {}
enum Boolean {True, False}
enum LastAction {proposedSolution, acceptedSolution, deniedSolution, idle}
sig Snapshot {
	var snapshot: one LastAction
}
sig User {}

sig Complaint {
    text: one Text,                     // A unique and necessary text
    var isSolved: one Boolean,             // Whether the complaint is solved
    var propose_solved: lone User       // The user proposing a solution (optional but lone)
}
fact complaintConstraints {
    all c: Complaint | {
        // Only one propose_solved, and only when isSolved is false
        (some c.propose_solved) => c.isSolved = False

        // Once isSolved is true, it stays true
        always (c.isSolved = True => c.isSolved' = True)
    }
}
fact "init" {
	all c : Complaint | {
		c.isSolved = False
		no propose_solved
	}
	all s : Snapshot | {
		s.snapshot = idle
	} 
}
pred userProposesSolution[c: Complaint, u: User, s: Snapshot] {
    // Preconditions
    c.isSolved = False       // Complaint must not be solved
    no c.propose_solved      // No current proposal

    // Effects
    c.propose_solved' = u    // Assign user to propose_solved
    s.snapshot = proposedSolution
}

pred userAcceptsSolution[c: Complaint, u: User, s: Snapshot] {
    // Preconditions
    c.isSolved = False                    // Complaint must not be solved
    some c.propose_solved                 // There must be a current proposal
    c.propose_solved != u                 // The accepting user must be different

    // Effects
    c.isSolved' = True                    // Mark complaint as solved
    no c.propose_solved'                  // Clear the proposal
    s.snapshot = acceptedSolution
}
pred userDeniesSolution[c: Complaint, u: User, s: Snapshot] {
    // Preconditions
    c.isSolved = False                    // Complaint must not be solved
    some c.propose_solved                 // There must be a current proposal
    c.propose_solved != u                 // The denying user must be different

    // Effects
    no c.propose_solved'                  // Clear the proposal
    s.snapshot = deniedSolution
}
fact enforcePredicates {
    all c: Complaint | {
        // A proposal is made only if `userProposesSolution` has occurred
        always ((some c.propose_solved' and no c.propose_solved) => some u: User, s: Snapshot | userProposesSolution[c, u, s])

        // The complaint is marked solved only if `userAcceptsSolution` has occurred
        always ((c.isSolved' = True and c.isSolved = False) => some u: User, s: Snapshot | userAcceptsSolution[c, u, s])

        // The proposal is cleared only if `userAcceptsSolution` or `userDeniesSolution` has occurred,
        // unless it's the initial state
        always ((no c.propose_solved' and some c.propose_solved) => some u: User, s: Snapshot | 
            userAcceptsSolution[c, u, s] or userDeniesSolution[c, u, s])
    }
}

pred show {
#Complaint = 1
#User = 2
#Snapshot = 1
let u1 = User, u2 = User, c = Complaint, s = Snapshot | {
	eventually userDeniesSolution[c, u1, s] and
	eventually userAcceptsSolution[c, u2, s] 
}
}
run show



















































































