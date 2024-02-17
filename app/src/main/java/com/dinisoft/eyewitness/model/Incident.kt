package com.dinisoft.eyewitness.model

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class Incident {

    private var IncidentID: String? = null
    private var DocumentID: String? = null
    private var IncidentType: String? = null
    private var IncidentTitle: String? = null
    private var IncidentMimeType: String? = null
    private var IncidentDesc: String? = null
    private var IncidentAddress: String? = null
    private var IncidentReportedTo: String? = null
    private var IncidentDate: String? = null
    private var IncidentLatitude: Double? = null
    private var IncidentLongitude: Double? = null
    private var IncidentEvidenceURL: String? = null
    private var IncidentReviewInfo: String? = null
    private var IncidentReviewStatus: String? = null
    private var IncidentReviewBy: String? = null
    private var IncidentSubType: String? = null
    private var IncidentHeadline: String? = null
    private var IncidentState: String? = null
    private var IncidentCountry: String? = null
    private var IncidentCount: String? = null

    // an empty constructor is
    // required when using
    // Firebase Realtime Database.
    fun Incident() {}

    // created getter and setter methods
    // for all our variables.
    fun getIncidentID(): String? {
        return IncidentID
    }

    fun setIncidentID(IncidentID: String?) {
        this.IncidentID = IncidentID
    }

    fun getDocumentID(): String? {
        return DocumentID
    }

    fun setDocumentID(DocumentID: String?) {
        this.DocumentID = DocumentID
    }

    fun getIncidentType(): String? {
        return IncidentType
    }

    fun setIncidentType(IncidentType: String?) {
        this.IncidentType = IncidentType
    }

    fun getIncidentMimeType(): String? {
        return IncidentMimeType
    }

    fun setIncidentMimeType(IncidentMimeType: String?) {
        this.IncidentMimeType = IncidentMimeType
    }

    fun getIncidentTitle(): String? {
        return IncidentTitle
    }

    fun setIncidentTitle(IncidentTitle: String?) {
        this.IncidentTitle = IncidentTitle
    }

    fun getIncidentDesc(): String? {
        return IncidentDesc
    }

    fun setIncidentDesc(IncidentDesc: String?) {
        this.IncidentDesc = IncidentDesc
    }

    fun getIncidentAddress(): String? {
        return IncidentAddress
    }

    fun setIncidentAddress(IncidentAddress: String?) {
        this.IncidentAddress = IncidentAddress
    }


    fun getIncidentReportedTo(): String? {
        return IncidentReportedTo
    }

    fun setIncidentReportedTo(IncidentReportedTo: String?) {
        this.IncidentReportedTo = IncidentReportedTo
    }

    fun getIncidentDate(): String? {
        return IncidentDate
    }

    fun setIncidentDate(IncidentDate: String?) {
        this.IncidentDate = IncidentDate
    }

    fun getIncidentLatitude(): Double? {
        return IncidentLatitude
    }

    fun setIncidentLatitude(IncidentLatitude: Double) {
        this.IncidentLatitude = IncidentLatitude
    }

    fun getIncidentLongitude(): Double? {
        return IncidentLongitude
    }

    fun setIncidentLongitude(IncidentLongitude: Double) {
        this.IncidentLongitude = IncidentLongitude
    }

    fun getIncidentEvidenceURL(): String? {
        return IncidentEvidenceURL
    }

    fun setIncidentEvidenceURL(IncidentEvidenceURL: String) {
        this.IncidentEvidenceURL = IncidentEvidenceURL.toString()
    }


    fun getIncidentReviewStatus(): String? {
        return IncidentReviewStatus
    }

    fun setIncidentReviewStatus(IncidentReviewStatus: String?) {
        this.IncidentReviewStatus = IncidentReviewStatus
    }

    fun getIncidentReviewInfo(): String? {
        return IncidentReviewInfo
    }

    fun setIncidentReviewInfo(IncidentReviewInfo: String?) {
        this.IncidentReviewInfo = IncidentReviewInfo
    }

    fun getIncidentReviewBy(): String? {
        return IncidentReviewBy
    }

    fun setIncidentReviewBy(IncidentReviewBy: String?) {
        this.IncidentReviewBy = IncidentReviewBy
    }

    fun getIncidentSubType(): String? {
        return IncidentSubType
    }

    fun setIncidentSubType(IncidentSubType: String?) {
        this.IncidentSubType = IncidentSubType
    }

    fun getIncidentHeadline(): String? {
        return IncidentHeadline
    }

    fun setIncidentHeadline(IncidentHeadline: String?) {
        this.IncidentHeadline = IncidentHeadline
    }

    fun getIncidentState(): String? {
        return IncidentState
    }

    fun setIncidentState(IncidentState: String) {
        this.IncidentState = IncidentState
    }

    fun getIncidentCountry(): String? {
        return IncidentCountry
    }

    fun setIncidentCountry(IncidentCountry: String) {
        this.IncidentCountry = IncidentCountry
    }

    fun getIncidentCount(): String? {
        return IncidentCount
    }

    fun setIncidentCount(IncidentCoun: String) {
        this.IncidentCount = IncidentCount
    }

}