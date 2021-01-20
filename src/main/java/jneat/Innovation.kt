package jneat

import jNeatCommon.NeatConstant

/**
 * This class serves as a way to record innovations
 * specifically, so that an innovation in one genome can be
 * compared with other innovations in the same epoch, and if they
 * Are the same innovation, they can both be assigned the same innnovation number.
 * This class can encode innovations that represent a new link
 * forming, or a new node being added.  In each case, two
 * nodes fully specify the innovation and where it must have
 * occured.  (Between them)
 */
class Innovation : Neat {
    /**
     * Either NEWNODE or NEWLINK
     */
    var innovation_type = 0

    /**
     * Two nodes specify where the innovation took place : this is the node input
     */
    var node_in_id = 0

    /**
     * Two nodes specify where the innovation took place : this is the node output
     */
    var node_out_id = 0

    /**
     * The number assigned to the innovation
     */
    var innovation_num1 = 0.0

    /**
     * If this is a new node innovation,then there are 2 innovations (links)
     * added for the new node
     */
    var innovation_num2 = 0.0

    /**
     * If a link is added, this is its weight
     */
    var new_weight = 0.0

    /**
     * If a link is added, this is its connected trait
     */
    var new_traitnum = 0

    /**
     * If a new node was created, this is its node_id
     */
    var newnode_id = 0

    /**
     * If a new node was created, this is
     * the innovnum of the gene's link it is being
     * stuck inside
     */
    var old_innov_num = 0.0

    /**
     * is recurrent ?
     */
    var recur_flag = false

    /**
     * Insert the method's description here.
     * Creation date: (24/01/2002 8.09.28)
     */
    constructor() {}

    /**
     * Insert the method's description here.
     * Creation date: (23/01/2002 9.04.02)
     */
    constructor(nin: Int, nout: Int, num1: Double, w: Double, t: Int) {
        innovation_type = NeatConstant.NEWLINK
        node_in_id = nin
        node_out_id = nout
        innovation_num1 = num1
        new_weight = w
        new_traitnum = t

        //Unused parameters set to zero
        innovation_num2 = 0.0
        newnode_id = 0
        recur_flag = false
    }

    /**
     * Insert the method's description here.
     * Creation date: (24/01/2002 8.09.28)
     */
    constructor(nin: Int, nout: Int, num1: Double, num2: Double, newid: Int, oldinnov: Double) {
        innovation_type = NeatConstant.NEWNODE
        node_in_id = nin
        node_out_id = nout
        innovation_num1 = num1
        innovation_num2 = num2
        newnode_id = newid
        old_innov_num = oldinnov

        //Unused parameters set to zero
        new_weight = 0.0
        new_traitnum = 0
        recur_flag = false
    }
}