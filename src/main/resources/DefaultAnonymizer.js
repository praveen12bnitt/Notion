// Default anonymization script


// Zero pad to 4 places
function pad (s) {
  var n = new java.lang.Integer ( s )
  // Zero pad
  return java.lang.String.format ( "%04d", n )
}


// The anonymizer expects a hash to be the last statement in this script.
// Any fields in the hash that map to DICOM tags will replace the tags in the
// saved DICOM.  The 'tags' hash contains the original tags for this DICOM file.
// Please see Notion's documentation for more details.

var tags = {
  PatientName: anonymizer.lookup('PatientName', tags.PatientName ) || 'PN-' + pad ( anonymizer.sequenceNumber ( 'PatientName', tags.PatientName) ),
  PatientID: anonymizer.lookup('PatientID', tags.PatientID ) || anonymizer.sequenceNumber ( 'PatientID', tags.PatientID),
  AccessionNumber: anonymizer.lookup('AccessionNumber', tags.AccessionNumber ) || anonymizer.sequenceNumber ( 'AccessionNumber', tags.AccessionNumber)
};

tags;
