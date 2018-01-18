<h2>Java Memory Usage (KB)</h2>

<div id="memory_usage">

</div>
<script type="text/javascript">
var timeout = 3000;
function loadTrackers() {
$.ajax({
	url:'index/memory_usage_table.jsp',
	cache:false,
	dataType: "html",
	success: function(data) {
		$('#memory_usage').empty();
		$('#memory_usage').html(data);
		setTimeout(loadTrackers, timeout);
	},
	error: function(e, xhr) {
		$('#memory_usage').html(e);
		setTimeout(loadTrackers, timeout);
	}
});
}
loadTrackers();
</script>
