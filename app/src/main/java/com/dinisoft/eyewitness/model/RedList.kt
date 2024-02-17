package com.dinisoft.eyewitness.model

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class RedList {

    private var authID: String? = null
    private var documentID: String? = null
    private var personName: String? = null
    private var personDOB: String? = null
    private var personSex: String? = null
    private var personRace: String? = null
    private var personBodyType: String? = null
    private var personBodyMark: String? = null
    private var personHairType: String? = null
    private var personOccupation: String? = null
    private var personLastAddress: String? = null
    private var personStatus: String? = null
    private var personDesc: String? = null
    private var personCaseDetails: String? = null
    private var personReward: String? = null
    private var personPhotograph: String? = null
    private var personMetatime: String? = null
    private var personSeekInfoRegion: String? = null
    private var authName: String? = null
    private var alertInfo: String? = null

    // an empty constructor is
    // required when using
    // Firebase Realtime Database.
    fun RedList() {}

    // created getter and setter methods
    // for all our variables.
    fun getAuthID(): String? {
        return authID
    }

    fun setAuthID(authID: String?) {
        this.authID = authID
    }

    fun getDocumentID(): String? {
        return documentID
    }

    fun setDocumentID(documentID: String?) {
        this.documentID = documentID
    }

    fun getPersonName(): String? {
        return personName
    }

    fun setPersonName(personName: String?) {
        this.personName = personName
    }

    fun getPersonDOB(): String? {
        return personDOB
    }

    fun setPersonDOB(personDOB: String?) {
        this.personDOB = personDOB
    }

    fun getPersonSex(): String? {
        return personSex
    }

    fun setPersonSex(personSex: String?) {
        this.personSex = personSex
    }

    fun getPersonRace(): String? {
        return personRace
    }

    fun setPersonBodyType(personBodyType: String?) {
        this.personBodyType = personBodyType
    }

    fun getPersonBodyMark(): String? {
        return personBodyMark
    }

    fun setPersonBodyMark(personBodyMark: String?) {
        this.personBodyMark = personBodyMark
    }


    fun getPersonHairType(): String? {
        return personHairType
    }

    fun setPersonHairType(personHairType: String?) {
        this.personHairType = personHairType
    }

    fun getPersonOccupation(): String? {
        return personOccupation
    }

    fun setPersonOccupation(personOccupation: String?) {
        this.personOccupation = personOccupation
    }

    fun getPersonLastAddress(): String? {
        return personLastAddress
    }

    fun setPersonLastAddress(personLastAddress: String?) {
        this.personLastAddress = personLastAddress
    }

    fun getPersonStatus(): String? {
        return personStatus
    }

    fun setPersonStatus(personStatus: String?) {
        this.personStatus = personStatus
    }

    fun getPersonDesc(): String? {
        return personDesc
    }

    fun setPersonDesc(personDesc: String) {
        this.personDesc = personDesc
    }


    fun getPersonCaseDetails(): String? {
        return personCaseDetails
    }

    fun setPersonCaseDetails(personCaseDetails: String?) {
        this.personCaseDetails = personCaseDetails
    }

    fun getPersonReward(): String? {
        return personReward
    }

    fun setPersonReward(personReward: String?) {
        this.personReward = personReward
    }

    fun getPersonPhotograph(): String? {
        return personPhotograph
    }

    fun setPersonPhotograph(personPhotograph: String?) {
        this.personPhotograph = personPhotograph
    }

    fun getPersonMetatime(): String? {
        return personMetatime
    }

    fun setPersonMetatime(personMetatime: String?) {
        this.personMetatime = personMetatime
    }

    fun getPersonSeekInfoRegion(): String? {
        return personSeekInfoRegion
    }

    fun setPersonSeekInfoRegion(personSeekInfoRegion: String?) {
        this.personSeekInfoRegion = personSeekInfoRegion
    }

    fun getAuthName(): String? {
        return authName
    }

    fun setAuthName(authName: String) {
        this.authName = authName
    }

    fun getAlertInfo(): String? {
        return alertInfo
    }

    fun setAlertInfo(alertInfo: String) {
        this.alertInfo = alertInfo
    }


}